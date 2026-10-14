package mogni.system_backend.notificacion.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/** El cifrado de las push tiene que dar exactamente lo que espera el estándar, o el navegador no lo puede leer. */
class WebPushSenderTest {

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    void cifrado_coincide_con_el_ejemplo_del_rfc_8291() throws Exception {
        WebPushSender sender = new WebPushSender("", "", "mailto:test@test.com");

        // Valores del apéndice A de RFC 8291
        byte[] mensaje = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8);
        byte[] clavePublicaServidor = WebPushSender.decodificar(
                "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
        byte[] clavePrivadaServidor = WebPushSender.decodificar("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw");
        byte[] clavePublicaDispositivo = WebPushSender.decodificar(
                "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] auth = WebPushSender.decodificar("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = WebPushSender.decodificar("DGv6ra1nlYgDCS1FRnbzlw");
        KeyPair efimero = new KeyPair(sender.clavePublicaDe(clavePublicaServidor), sender.clavePrivadaDe(clavePrivadaServidor));

        byte[] cuerpo = sender.cifrar(mensaje, clavePublicaDispositivo, auth, efimero, salt);

        assertEquals("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN",
                b64(cuerpo));
    }

    @Test
    void la_clave_publica_se_convierte_ida_y_vuelta_sin_cambios() throws Exception {
        WebPushSender sender = new WebPushSender("", "", "mailto:test@test.com");
        KeyPair par = sender.generarParDeClaves();
        byte[] bytes = WebPushSender.aBytes((java.security.interfaces.ECPublicKey) par.getPublic());

        assertEquals(65, bytes.length);
        assertArrayEquals(bytes, WebPushSender.aBytes(sender.clavePublicaDe(bytes)));
    }

    @Test
    void sin_claves_vapid_las_push_quedan_desactivadas() throws Exception {
        assertFalse(new WebPushSender("", "", "mailto:test@test.com").estaConfigurado());
        assertTrue(new WebPushSender("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4",
                "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94", "mailto:test@test.com").estaConfigurado());
    }
}
