package mogni.system_backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.auth.dto.RestablecerPasswordRequest;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.TokenRecuperacionPassword;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.auth.repository.TokenRecuperacionPasswordRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.mail.BrevoMailSender;
import mogni.system_backend.mail.MailNoEnviadoException;
import mogni.system_backend.mail.PlantillaMail;

/**
 * Recuperar contraseña («¿Olvidaste tu contraseña?»):
 * <ol>
 *   <li>el usuario pide el restablecimiento con su email y recibe un link por mail (Brevo);</li>
 *   <li>con ese link elige una contraseña nueva.</li>
 * </ol>
 * Reglas de seguridad:
 * <ul>
 *   <li>la respuesta del paso 1 es siempre la misma, exista o no el email, para no revelar qué cuentas existen;</li>
 *   <li>el token es aleatorio (256 bits), de un solo uso, vence a los {@code minutosValidez} minutos y en la base se
 *       guarda solo su hash;</li>
 *   <li>un pedido nuevo invalida los anteriores, y hay un máximo de pedidos seguidos por cuenta;</li>
 *   <li>las cuentas dadas de baja no pueden recuperar la contraseña.</li>
 * </ul>
 */
@Service
public class RecuperacionPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordService.class);

    /** Máximo de pedidos por cuenta dentro de la ventana (los demás se ignoran en silencio). */
    static final int MAXIMO_PEDIDOS = 3;
    static final int VENTANA_MINUTOS = 15;
    static final String ASUNTO = "Restablecé tu contraseña de Mogni Sport";
    public static final String MENSAJE_SOLICITUD =
            "Si el correo está registrado, te enviamos un link para restablecer la contraseña. Revisá también la carpeta de spam.";
    static final String MENSAJE_LINK_INVALIDO =
            "El link para restablecer la contraseña venció o ya se usó. Pedí uno nuevo.";

    private final CuentaRepository cuentaRepository;
    private final TokenRecuperacionPasswordRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoMailSender mailSender;
    private final String frontendUrl;
    private final int minutosValidez;
    private final SecureRandom random = new SecureRandom();

    public RecuperacionPasswordService(CuentaRepository cuentaRepository,
                                       TokenRecuperacionPasswordRepository tokenRepository,
                                       PasswordEncoder passwordEncoder,
                                       BrevoMailSender mailSender,
                                       @Value("${app.frontend-url}") String frontendUrl,
                                       @Value("${app.recuperacion-password.minutos-validez:30}") int minutosValidez) {
        this.cuentaRepository = cuentaRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl.trim().replaceAll("/+$", "");
        this.minutosValidez = minutosValidez;
    }

    // --- Paso 1: pedir el link ---

    @Transactional
    public void solicitar(String email) {
        solicitar(email, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    void solicitar(String email, LocalDateTime ahora) {
        Optional<Cuenta> encontrada = cuentaRepository.findByEmailIgnoreCase(email.trim());
        if (encontrada.isEmpty() || !encontrada.get().isActiva()) {
            log.info("Recuperar contraseña: pedido para un email sin cuenta activa (no se envía mail)");
            return;
        }
        Cuenta cuenta = encontrada.get();
        if (tokenRepository.countByCuentaIdAndCreadoEnAfter(cuenta.getId(), ahora.minusMinutes(VENTANA_MINUTOS))
                >= MAXIMO_PEDIDOS) {
            log.warn("Recuperar contraseña: la cuenta {} superó el máximo de pedidos seguidos", cuenta.getId());
            return;
        }

        invalidarPendientes(cuenta, ahora);
        String token = generarToken();
        tokenRepository.save(new TokenRecuperacionPassword(cuenta, hash(token), ahora, ahora.plusMinutes(minutosValidez)));

        String link = frontendUrl + "/restablecer-password?token=" + token;
        if (!mailSender.estaConfigurado()) {
            // Solo pasa sin Brevo configurado (pruebas locales): así se puede seguir el flujo desde el log
            log.warn("Brevo no está configurado: no se envió el mail. Link para restablecer la contraseña: {}", link);
            return;
        }
        Map<String, String> valores = Map.of("link", link, "minutos", String.valueOf(minutosValidez));
        try {
            mailSender.enviar(cuenta.getEmail(), ASUNTO,
                    PlantillaMail.completar("restablecer-password.html", valores),
                    PlantillaMail.completar("restablecer-password.txt", valores));
        } catch (MailNoEnviadoException e) {
            // No se informa al usuario (revelaría que el email existe): queda en el log para el equipo
            log.error("Recuperar contraseña: no se pudo enviar el mail a la cuenta {}: {}", cuenta.getId(), e.getMessage());
        }
    }

    // --- Paso 2: elegir la contraseña nueva ---

    /** Para que la pantalla avise antes de completar el formulario si el link ya no sirve. */
    @Transactional(readOnly = true)
    public void validar(String token) {
        buscarValido(token, ZonaHoraria.ahora());
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request) {
        restablecer(request, ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    void restablecer(RestablecerPasswordRequest request, LocalDateTime ahora) {
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new RegistroInvalidoException("La confirmación no coincide con la contraseña nueva");
        }
        TokenRecuperacionPassword pedido = buscarValido(request.getToken(), ahora);
        Cuenta cuenta = pedido.getCuenta();

        cuenta.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        // La eligió el usuario: ya no tiene que cambiarla al entrar (aunque fuera la inicial, el DNI)
        cuenta.setDebeCambiarPassword(false);
        cuentaRepository.save(cuenta);
        invalidarPendientes(cuenta, ahora);
        log.info("Recuperar contraseña: la cuenta {} restableció su contraseña", cuenta.getId());
    }

    private TokenRecuperacionPassword buscarValido(String token, LocalDateTime ahora) {
        if (token == null || token.isBlank()) {
            throw new RegistroInvalidoException(MENSAJE_LINK_INVALIDO);
        }
        return tokenRepository.findByTokenHash(hash(token.trim()))
                .filter(pedido -> pedido.esValido(ahora) && pedido.getCuenta().isActiva())
                .orElseThrow(() -> new RegistroInvalidoException(MENSAJE_LINK_INVALIDO));
    }

    /** Un link nuevo o un cambio de contraseña dejan sin efecto los links anteriores. */
    private void invalidarPendientes(Cuenta cuenta, LocalDateTime ahora) {
        for (TokenRecuperacionPassword pendiente : tokenRepository.findByCuentaIdAndUsadoEnIsNull(cuenta.getId())) {
            pendiente.marcarUsado(ahora);
            tokenRepository.save(pendiente);
        }
    }

    /** 32 bytes aleatorios en base64url (43 caracteres, se pueden poner en una URL sin escapar). */
    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
