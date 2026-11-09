package mogni.system_backend.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.notificacion.dto.SuscripcionPushRequest;
import mogni.system_backend.notificacion.dto.TipoNotificacion;
import mogni.system_backend.notificacion.entity.AvisoPushEnviado;
import mogni.system_backend.notificacion.entity.SuscripcionPush;
import mogni.system_backend.notificacion.repository.AvisoPushEnviadoRepository;
import mogni.system_backend.notificacion.repository.AvisoTurnoReactivadoRepository;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;
import mogni.system_backend.notificacion.repository.SuscripcionPushRepository;
import mogni.system_backend.notificacion.service.NotificacionService;
import mogni.system_backend.notificacion.service.PushService;
import mogni.system_backend.notificacion.service.WebPushSender;

/** HU-250 (push): cada cuota impaga se avisa al celular una sola vez por estado ("por vencer" y "vencida"). */
@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    private static final String EMAIL = "martin@gmail.com";

    @Mock private WebPushSender sender;
    @Mock private SuscripcionPushRepository suscripcionRepository;
    @Mock private AvisoPushEnviadoRepository avisoRepository;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private AvisoTurnoRepository avisoTurnoRepository;
    @Mock private AvisoTurnoReactivadoRepository avisoTurnoReactivadoRepository;

    private PushService pushService;
    private final LocalDate hoy = ZonaHoraria.hoy();
    private Cuenta cuenta;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        // NotificacionService real: la push usa exactamente las mismas reglas que la app
        NotificacionService notificacionService = new NotificacionService(
                alumnoRepository, cuotaRepository, avisoTurnoRepository, avisoTurnoReactivadoRepository);
        pushService = new PushService(sender, suscripcionRepository, avisoRepository, cuentaRepository,
                cuotaRepository, notificacionService);
        lenient().when(sender.estaConfigurado()).thenReturn(true);

        cuenta = new Cuenta();
        cuenta.setId(3L);
        cuenta.setEmail(EMAIL);
        alumno = new Alumno();
        alumno.setId(1L);
        alumno.setCuenta(cuenta);
    }

    /** Última cuota del alumno, que vence el día indicado (la situación de pago se calcula con esa fecha). */
    private Cuota cuota(Long id, LocalDate vencimiento) {
        Membresia membresia = new Membresia(7L, "HIIT", 25000.0, 3, null);
        Inscripcion inscripcion = new Inscripcion(5L, vencimiento.minusDays(60), null, EstadoInscripcion.ACTIVA, alumno, membresia);
        Cuota cuota = inscripcion.agregarCuota(vencimiento.minusDays(ReglasCuota.DIAS_CUOTA));
        cuota.setId(id);
        return cuota;
    }

    private SuscripcionPush dispositivo(String endpoint) {
        SuscripcionPush s = new SuscripcionPush();
        s.setId(9L);
        s.setCuenta(cuenta);
        s.setEndpoint(endpoint);
        s.setP256dh("clave");
        s.setAuth("auth");
        return s;
    }

    @Test
    void manda_la_push_de_una_cuota_por_vencer_y_la_registra_como_enviada() throws Exception {
        when(cuotaRepository.buscarUltimasCuotas())
                .thenReturn(Map.of(1L, cuota(10L, hoy.plusDays(3))));
        when(suscripcionRepository.findByCuentaId(3L)).thenReturn(List.of(dispositivo("https://push/1")));
        when(sender.enviar(eq("https://push/1"), anyString(), anyString(), anyString())).thenReturn(new WebPushSender.Resultado(201));

        pushService.enviarAvisosDeCuotas();

        ArgumentCaptor<AvisoPushEnviado> aviso = ArgumentCaptor.forClass(AvisoPushEnviado.class);
        verify(avisoRepository).save(aviso.capture());
        assertEquals(10L, aviso.getValue().getCuotaId());
        assertEquals(TipoNotificacion.CUOTA_POR_VENCER, aviso.getValue().getTipo());
    }

    @Test
    void no_se_repite_la_push_si_ya_se_aviso_esa_cuota_en_ese_estado() throws Exception {
        when(cuotaRepository.buscarUltimasCuotas())
                .thenReturn(Map.of(1L, cuota(10L, hoy.minusDays(2))));
        when(avisoRepository.existsByCuotaIdAndTipo(10L, TipoNotificacion.CUOTA_VENCIDA)).thenReturn(true);

        pushService.enviarAvisosDeCuotas();

        verify(sender, never()).enviar(anyString(), anyString(), anyString(), anyString());
        verify(avisoRepository, never()).save(any());
    }

    @Test
    void alumno_al_dia_no_genera_push() throws Exception {
        when(cuotaRepository.buscarUltimasCuotas())
                .thenReturn(Map.of(1L, cuota(10L, hoy.plusDays(20))));

        pushService.enviarAvisosDeCuotas();

        verify(sender, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void si_el_alumno_no_tiene_dispositivos_no_se_marca_como_enviada_para_avisarle_cuando_active() {
        when(cuotaRepository.buscarUltimasCuotas())
                .thenReturn(Map.of(1L, cuota(10L, hoy.minusDays(2))));
        when(suscripcionRepository.findByCuentaId(3L)).thenReturn(List.of());

        pushService.enviarAvisosDeCuotas();

        verify(avisoRepository, never()).save(any());
    }

    @Test
    void dispositivo_dado_de_baja_se_borra_y_no_cuenta_como_enviada() throws Exception {
        when(cuotaRepository.buscarUltimasCuotas())
                .thenReturn(Map.of(1L, cuota(10L, hoy.minusDays(2))));
        when(suscripcionRepository.findByCuentaId(3L)).thenReturn(List.of(dispositivo("https://push/viejo")));
        when(sender.enviar(eq("https://push/viejo"), anyString(), anyString(), anyString())).thenReturn(new WebPushSender.Resultado(410));

        pushService.enviarAvisosDeCuotas();

        verify(suscripcionRepository).deleteByEndpoint("https://push/viejo");
        verify(avisoRepository, never()).save(any());
    }

    @Test
    void sin_claves_vapid_la_tarea_no_hace_nada() {
        when(sender.estaConfigurado()).thenReturn(false);

        pushService.enviarAvisosDeCuotas();

        verify(cuotaRepository, never()).buscarUltimasCuotas();
    }

    @Test
    void suscribir_reasigna_el_dispositivo_a_la_cuenta_que_lo_usa_ahora() {
        Cuenta otra = new Cuenta();
        otra.setId(99L);
        SuscripcionPush existente = dispositivo("https://push/1");
        existente.setCuenta(otra);
        when(cuentaRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(cuenta));
        when(suscripcionRepository.findByEndpoint("https://push/1")).thenReturn(Optional.of(existente));

        SuscripcionPushRequest request = new SuscripcionPushRequest();
        request.setEndpoint("https://push/1");
        request.setP256dh("nueva");
        request.setAuth("nuevo");
        pushService.suscribir(EMAIL, request);

        verify(suscripcionRepository).save(existente);
        assertEquals(cuenta, existente.getCuenta());
        assertEquals("nueva", existente.getP256dh());
    }

    @Test
    void prueba_sin_dispositivos_avisa_que_hay_que_activarlas() {
        when(suscripcionRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(List.of());

        assertThrows(RegistroInvalidoException.class, () -> pushService.enviarPrueba(EMAIL));
    }

    @Test
    void prueba_manda_las_notificaciones_vigentes_a_cada_dispositivo() throws Exception {
        when(suscripcionRepository.findByCuentaEmailIgnoreCase(EMAIL))
                .thenReturn(List.of(dispositivo("https://push/1"), dispositivo("https://push/2")));
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscarUltimaCuota(1L))
                .thenReturn(Optional.of(cuota(10L, hoy.minusDays(2))));
        when(sender.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(new WebPushSender.Resultado(201));

        assertEquals(2, pushService.enviarPrueba(EMAIL));
        verify(sender, times(2)).enviar(anyString(), anyString(), anyString(), anyString());
        // La prueba no consume el aviso: la tarea diaria igual lo manda
        verify(avisoRepository, never()).save(any());
    }

    @Test
    void aviso_de_turno_llega_a_cada_dispositivo_de_la_cuenta_con_su_tag() throws Exception {
        when(suscripcionRepository.findByCuentaId(3L))
                .thenReturn(List.of(dispositivo("https://push/1"), dispositivo("https://push/2")));
        when(sender.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(new WebPushSender.Resultado(201));

        assertEquals(2, pushService.enviarAviso(3L, "turno-7", "Turno cancelado por el gimnasio", "Se canceló tu turno"));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).enviar(anyString(), anyString(), anyString(), mensaje.capture());
        Map<?, ?> json = new tools.jackson.databind.json.JsonMapper().readValue(mensaje.getValue(), Map.class);
        assertEquals("Turno cancelado por el gimnasio", json.get("titulo"));
        assertEquals("Se canceló tu turno", json.get("mensaje"));
        assertEquals("turno-7", json.get("tag"));
        assertEquals("/mi-cuenta/notificaciones", json.get("url"));
    }

    @Test
    void aviso_de_turno_sigue_con_los_demas_dispositivos_si_uno_falla() throws Exception {
        when(suscripcionRepository.findByCuentaId(3L))
                .thenReturn(List.of(dispositivo("https://push/1"), dispositivo("https://push/2")));
        when(sender.enviar(eq("https://push/1"), anyString(), anyString(), anyString()))
                .thenThrow(new java.io.IOException("sin conexión"));
        when(sender.enviar(eq("https://push/2"), anyString(), anyString(), anyString()))
                .thenReturn(new WebPushSender.Resultado(201));

        assertEquals(1, pushService.enviarAviso(3L, "turno-7", "Turno cancelado por el gimnasio", "Se canceló tu turno"));
    }

    @Test
    void aviso_de_turno_sin_push_configuradas_no_manda_nada() throws Exception {
        when(sender.estaConfigurado()).thenReturn(false);

        assertEquals(0, pushService.enviarAviso(3L, "turno-7", "Turno cancelado por el gimnasio", "Se canceló tu turno"));
        verify(sender, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }
}
