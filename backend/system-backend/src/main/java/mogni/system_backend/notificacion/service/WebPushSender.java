package mogni.system_backend.notificacion.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Envía un mensaje push a un dispositivo usando el protocolo estándar Web Push, solo con la criptografía de Java:
 * <ul>
 *   <li>El mensaje va cifrado para ese dispositivo (RFC 8291, "aes128gcm"): solo el navegador puede leerlo.</li>
 *   <li>Cada envío va firmado con las claves VAPID del servidor (RFC 8292), que lo identifican ante el servicio
 *       de push (Google, Apple, Mozilla). La clave pública es la misma que usa el frontend al suscribirse.</li>
 * </ul>
 * Las claves se configuran con push.vapid.public-key / push.vapid.private-key. Si faltan, las push quedan
 * desactivadas y el resto de la aplicación funciona igual.
 */
@Component
public class WebPushSender {

    /** Respuesta del servicio de push. 404/410: el dispositivo ya no existe (se desinstaló o revocó el permiso). */
    public record Resultado(int status) {
        public boolean enviado() {
            return status >= 200 && status < 300;
        }

        public boolean suscripcionVencida() {
            return status == 404 || status == 410;
        }
    }

    private static final int TAMANIO_REGISTRO = 4096;
    private static final int SEGUNDOS_DE_VIDA = 24 * 60 * 60; // si el celular está apagado, se entrega hasta 1 día después
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final ECParameterSpec curva;
    private final SecureRandom random = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String sujeto;
    private final String clavePublicaVapid;
    private final ECPrivateKey clavePrivadaVapid;

    public WebPushSender(@Value("${push.vapid.public-key:}") String clavePublica,
                         @Value("${push.vapid.private-key:}") String clavePrivada,
                         @Value("${push.vapid.subject:mailto:admin@mognisport.com}") String sujeto)
            throws GeneralSecurityException {
        this.curva = parametrosP256();
        this.sujeto = sujeto;
        if (clavePublica.isBlank() || clavePrivada.isBlank()) {
            this.clavePublicaVapid = null;
            this.clavePrivadaVapid = null;
        } else {
            this.clavePublicaVapid = clavePublica.trim();
            this.clavePrivadaVapid = (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, decodificar(clavePrivada)), curva));
        }
    }

    public boolean estaConfigurado() {
        return clavePrivadaVapid != null;
    }

    /** Clave pública VAPID (base64url), la que el navegador necesita para suscribirse. */
    public String getClavePublica() {
        return clavePublicaVapid;
    }

    public Resultado enviar(String endpoint, String p256dh, String auth, String mensaje)
            throws IOException, InterruptedException, GeneralSecurityException {
        if (!estaConfigurado()) {
            throw new IllegalStateException("Las notificaciones push no están configuradas (faltan las claves VAPID)");
        }
        byte[] cuerpo = cifrar(mensaje.getBytes(StandardCharsets.UTF_8), decodificar(p256dh), decodificar(auth),
                generarParDeClaves(), nuevoSalt());

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("TTL", String.valueOf(SEGUNDOS_DE_VIDA))
                .header("Urgency", "normal")
                .header("Content-Type", "application/octet-stream")
                .header("Content-Encoding", "aes128gcm")
                .header("Authorization", "vapid t=" + firmaVapid(endpoint) + ", k=" + clavePublicaVapid)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                .build();
        return new Resultado(http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    /**
     * Cifrado "aes128gcm" de RFC 8291. Recibe la clave efímera y el salt para poder probarlo con el ejemplo
     * del apéndice A del RFC; en un envío real son aleatorios cada vez.
     */
    byte[] cifrar(byte[] mensaje, byte[] clavePublicaDispositivo, byte[] secretoAuth, KeyPair efimero, byte[] salt)
            throws GeneralSecurityException {
        byte[] clavePublicaServidor = aBytes((ECPublicKey) efimero.getPublic());

        KeyAgreement ecdh = KeyAgreement.getInstance("ECDH");
        ecdh.init(efimero.getPrivate());
        ecdh.doPhase(clavePublicaDe(clavePublicaDispositivo), true);
        byte[] secretoCompartido = ecdh.generateSecret();

        // Se mezcla el secreto compartido con el secreto "auth" del dispositivo...
        byte[] infoClave = concatenar("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0},
                clavePublicaDispositivo, clavePublicaServidor);
        byte[] ikm = hkdf(secretoAuth, secretoCompartido, infoClave, 32);
        // ...y de ahí salen la clave de contenido y el nonce del AES-GCM
        byte[] cek = hkdf(salt, ikm, concatenar("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] {0}), 16);
        byte[] nonce = hkdf(salt, ikm, concatenar("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] {0}), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] cifrado = aes.doFinal(concatenar(mensaje, new byte[] {2})); // 0x02: último (y único) registro

        ByteBuffer encabezado = ByteBuffer.allocate(16 + 4 + 1 + clavePublicaServidor.length)
                .put(salt)
                .putInt(TAMANIO_REGISTRO)
                .put((byte) clavePublicaServidor.length)
                .put(clavePublicaServidor);
        return concatenar(encabezado.array(), cifrado);
    }

    /** JWT firmado con ES256 que identifica al servidor ante el servicio de push de ese endpoint. */
    private String firmaVapid(String endpoint) throws GeneralSecurityException {
        URI uri = URI.create(endpoint);
        String audiencia = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        long vence = Instant.now().plus(Duration.ofHours(12)).getEpochSecond();

        String encabezado = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String datos = B64.encodeToString(("{\"aud\":\"" + audiencia + "\",\"exp\":" + vence + ",\"sub\":\"" + sujeto + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        String contenido = encabezado + "." + datos;

        Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format"); // r||s de 64 bytes, como pide JWT
        firma.initSign(clavePrivadaVapid);
        firma.update(contenido.getBytes(StandardCharsets.US_ASCII));
        return contenido + "." + B64.encodeToString(firma.sign());
    }

    // --- Utilidades de claves y HKDF ---

    /** HKDF (RFC 5869) con HMAC-SHA-256; alcanza con un solo bloque porque nunca se piden más de 32 bytes. */
    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int largo) throws GeneralSecurityException {
        byte[] prk = hmac(salt, ikm);
        return Arrays.copyOf(hmac(prk, concatenar(info, new byte[] {1})), largo);
    }

    private static byte[] hmac(byte[] clave, byte[] datos) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(clave, "HmacSHA256"));
        return mac.doFinal(datos);
    }

    KeyPair generarParDeClaves() throws GeneralSecurityException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp256r1"), random);
        return generador.generateKeyPair();
    }

    private byte[] nuevoSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private static ECParameterSpec parametrosP256() throws GeneralSecurityException {
        AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
        parametros.init(new ECGenParameterSpec("secp256r1"));
        return parametros.getParameterSpec(ECParameterSpec.class);
    }

    /** Punto sin comprimir (0x04 || X || Y, 65 bytes), el formato en que los navegadores mandan las claves. */
    ECPublicKey clavePublicaDe(byte[] punto) throws GeneralSecurityException {
        if (punto.length != 65 || punto[0] != 0x04) {
            throw new GeneralSecurityException("Clave pública del dispositivo inválida");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(punto, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(punto, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curva));
    }

    ECPrivateKey clavePrivadaDe(byte[] d) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), curva));
    }

    static byte[] aBytes(ECPublicKey clave) {
        return concatenar(new byte[] {0x04}, a32Bytes(clave.getW().getAffineX()), a32Bytes(clave.getW().getAffineY()));
    }

    private static byte[] a32Bytes(BigInteger n) {
        byte[] bytes = n.toByteArray(); // puede venir con un 0 adelante o con menos de 32 bytes
        byte[] resultado = new byte[32];
        int largo = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - largo, resultado, 32 - largo, largo);
        return resultado;
    }

    /** Acepta base64url con o sin relleno, y también base64 común. */
    static byte[] decodificar(String base64) {
        String normalizado = base64.trim().replace('+', '-').replace('/', '_').replace("=", "");
        return Base64.getUrlDecoder().decode(normalizado);
    }

    private static byte[] concatenar(byte[]... partes) {
        int total = 0;
        for (byte[] parte : partes) total += parte.length;
        ByteBuffer buffer = ByteBuffer.allocate(total);
        for (byte[] parte : partes) buffer.put(parte);
        return buffer.array();
    }
}
