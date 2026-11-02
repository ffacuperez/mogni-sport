package mogni.system_backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import mogni.system_backend.auth.dto.RestablecerPasswordRequest;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.entity.TokenRecuperacionPassword;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.auth.repository.TokenRecuperacionPasswordRepository;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.mail.BrevoMailSender;
import mogni.system_backend.mail.MailNoEnviadoException;

/**
 * Recuperar contraseña. Los repositorios se simulan con una lista de pedidos en memoria y el envío de mails con un
 * mock de BrevoMailSender (nunca se llama a Brevo). "Ahora" es el lunes 02/11/2026 10:00.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final String EMAIL = "lucia@mail.com";

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TokenRecuperacionPasswordRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BrevoMailSender mailSender;

    private RecuperacionPasswordService service;
    private Cuenta cuenta;
    private final List<TokenRecuperacionPassword> pedidos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new RecuperacionPasswordService(cuentaRepository, tokenRepository, passwordEncoder, mailSender,
                "https://mogni-sport-dev.vercel.app/", 30);
        cuenta = new Cuenta(1L, EMAIL, "hash-vieja", Rol.ALUMNO, true, true, null);

        lenient().when(cuentaRepository.findByEmailIgnoreCase(anyString()))
                .thenAnswer(inv -> EMAIL.equalsIgnoreCase(inv.getArgument(0)) ? Optional.of(cuenta) : Optional.empty());
        lenient().when(tokenRepository.save(any(TokenRecuperacionPassword.class))).thenAnswer(inv -> {
            TokenRecuperacionPassword pedido = inv.getArgument(0);
            if (!pedidos.contains(pedido)) {
                pedidos.add(pedido);
            }
            return pedido;
        });
        lenient().when(tokenRepository.findByTokenHash(anyString())).thenAnswer(inv -> pedidos.stream()
                .filter(p -> p.getTokenHash().equals(inv.getArgument(0))).findFirst());
        lenient().when(tokenRepository.findByCuentaIdAndUsadoEnIsNull(anyLong())).thenAnswer(inv -> pedidos.stream()
                .filter(p -> p.getUsadoEn() == null).toList());
        lenient().when(tokenRepository.countByCuentaIdAndCreadoEnAfter(anyLong(), any(LocalDateTime.class)))
                .thenAnswer(inv -> pedidos.stream()
                        .filter(p -> p.getCreadoEn().isAfter(inv.getArgument(1))).count());
        lenient().when(mailSender.estaConfigurado()).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash-" + inv.getArgument(0));
    }

    /** Pide el link y devuelve el token que viajó en el mail (sacado del link del HTML enviado). */
    private String pedirLink(LocalDateTime cuando) {
        service.solicitar(EMAIL, cuando);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).enviar(eq(EMAIL), anyString(), html.capture(), anyString());
        String contenido = html.getValue();
        int inicio = contenido.indexOf("token=") + "token=".length();
        return contenido.substring(inicio, contenido.indexOf('"', inicio));
    }

    private RestablecerPasswordRequest restablecer(String token, String nueva, String confirmacion) {
        return new RestablecerPasswordRequest(token, nueva, confirmacion);
    }

    // --- Paso 1: pedir el link ---

    @Test
    void con_un_email_registrado_envia_el_mail_con_el_link() {
        String token = pedirLink(AHORA);

        assertEquals(43, token.length());
        TokenRecuperacionPassword pedido = pedidos.get(0);
        assertEquals(cuenta, pedido.getCuenta());
        assertEquals(AHORA.plusMinutes(30), pedido.getVenceEn());
        // En la base queda el hash, no el token
        assertEquals(RecuperacionPasswordService.hash(token), pedido.getTokenHash());
        assertFalse(pedido.getTokenHash().contains(token));
    }

    @Test
    void el_link_apunta_al_frontend_configurado() {
        service.solicitar(EMAIL, AHORA);

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(mailSender).enviar(eq(EMAIL), eq(RecuperacionPasswordService.ASUNTO), anyString(), texto.capture());
        assertTrue(texto.getValue().contains("https://mogni-sport-dev.vercel.app/restablecer-password?token="));
        assertTrue(texto.getValue().contains("30 minutos"));
    }

    @Test
    void el_email_no_distingue_mayusculas_ni_espacios() {
        service.solicitar("  LUCIA@mail.com ", AHORA);

        verify(mailSender).enviar(eq(EMAIL), anyString(), anyString(), anyString());
    }

    @Test
    void con_un_email_no_registrado_no_envia_nada_ni_falla() {
        service.solicitar("nadie@mail.com", AHORA);

        verify(mailSender, never()).enviar(anyString(), anyString(), anyString(), anyString());
        assertTrue(pedidos.isEmpty());
    }

    @Test
    void con_una_cuenta_dada_de_baja_no_envia_nada() {
        cuenta.setActiva(false);

        service.solicitar(EMAIL, AHORA);

        verify(mailSender, never()).enviar(anyString(), anyString(), anyString(), anyString());
        assertTrue(pedidos.isEmpty());
    }

    @Test
    void un_pedido_nuevo_invalida_el_link_anterior() {
        String primero = pedirLink(AHORA);
        pedirLink(AHORA.plusMinutes(1));

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(primero, "nuevaSegura1", "nuevaSegura1"), AHORA.plusMinutes(2)));
        assertEquals(RecuperacionPasswordService.MENSAJE_LINK_INVALIDO, error.getMessage());
    }

    @Test
    void despues_de_tres_pedidos_seguidos_no_envia_mas_mails() {
        for (int i = 0; i < 4; i++) {
            service.solicitar(EMAIL, AHORA.plusMinutes(i));
        }

        verify(mailSender, org.mockito.Mockito.times(3)).enviar(anyString(), anyString(), anyString(), anyString());
        assertEquals(3, pedidos.size());
    }

    @Test
    void pasada_la_ventana_se_puede_volver_a_pedir() {
        for (int i = 0; i < 3; i++) {
            service.solicitar(EMAIL, AHORA.plusMinutes(i));
        }

        service.solicitar(EMAIL, AHORA.plusMinutes(20));

        assertEquals(4, pedidos.size());
    }

    @Test
    void si_brevo_falla_no_se_informa_al_usuario() {
        doThrow(new MailNoEnviadoException("Brevo respondió 401"))
                .when(mailSender).enviar(anyString(), anyString(), anyString(), anyString());

        service.solicitar(EMAIL, AHORA);

        assertEquals(1, pedidos.size());
    }

    @Test
    void sin_brevo_configurado_no_intenta_enviar() {
        lenient().when(mailSender.estaConfigurado()).thenReturn(false);

        service.solicitar(EMAIL, AHORA);

        verify(mailSender, never()).enviar(anyString(), anyString(), anyString(), anyString());
        assertEquals(1, pedidos.size());
    }

    // --- Paso 2: elegir la contraseña nueva ---

    @Test
    void con_el_link_valido_cambia_la_contrasenia() {
        String token = pedirLink(AHORA);

        service.restablecer(restablecer(token, "nuevaSegura1", "nuevaSegura1"), AHORA.plusMinutes(10));

        assertEquals("hash-nuevaSegura1", cuenta.getPassword());
        assertFalse(cuenta.isDebeCambiarPassword());
        assertNotNull(pedidos.get(0).getUsadoEn());
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    void el_link_se_puede_usar_una_sola_vez() {
        String token = pedirLink(AHORA);
        service.restablecer(restablecer(token, "nuevaSegura1", "nuevaSegura1"), AHORA.plusMinutes(10));

        assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(token, "otraSegura22", "otraSegura22"), AHORA.plusMinutes(11)));
        assertEquals("hash-nuevaSegura1", cuenta.getPassword());
    }

    @Test
    void el_link_vence_a_los_30_minutos() {
        String token = pedirLink(AHORA);

        service.restablecer(restablecer(token, "nuevaSegura1", "nuevaSegura1"), AHORA.plusMinutes(29));
        String otro = pedirLink(AHORA.plusHours(1));

        assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(otro, "otraSegura22", "otraSegura22"),
                        AHORA.plusHours(1).plusMinutes(30)));
    }

    @Test
    void un_token_inventado_es_invalido() {
        pedirLink(AHORA);

        assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer("inventado", "nuevaSegura1", "nuevaSegura1"), AHORA));
        assertEquals("hash-vieja", cuenta.getPassword());
    }

    @Test
    void la_confirmacion_tiene_que_coincidir() {
        String token = pedirLink(AHORA);

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(token, "nuevaSegura1", "nuevaSegura2"), AHORA));

        assertEquals("La confirmación no coincide con la contraseña nueva", error.getMessage());
        assertNull(pedidos.get(0).getUsadoEn());
    }

    @Test
    void una_cuenta_dada_de_baja_despues_del_pedido_no_puede_restablecer() {
        String token = pedirLink(AHORA);
        cuenta.setActiva(false);

        assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(token, "nuevaSegura1", "nuevaSegura1"), AHORA));
    }

    @Test
    void sin_token_es_invalido() {
        assertThrows(RegistroInvalidoException.class,
                () -> service.restablecer(restablecer(" ", "nuevaSegura1", "nuevaSegura1"), AHORA));
    }
}
