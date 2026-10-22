package mogni.system_backend.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.dto.TipoNotificacion;
import mogni.system_backend.notificacion.entity.AvisoTurno;
import mogni.system_backend.notificacion.entity.AvisoTurnoReactivado;
import mogni.system_backend.notificacion.repository.AvisoTurnoReactivadoRepository;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;
import mogni.system_backend.notificacion.service.NotificacionService;

/**
 * HU-250: el alumno ve una notificación por cada cuota impaga próxima a vencer (5 días o menos) o vencida,
 * y deja de verla cuando la cuota pasa a PAGADA.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final String EMAIL = "martin@gmail.com";

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private AvisoTurnoRepository avisoTurnoRepository;

    @Mock
    private AvisoTurnoReactivadoRepository avisoTurnoReactivadoRepository;

    @InjectMocks
    private NotificacionService notificacionService;

    private final LocalDate hoy = ZonaHoraria.hoy();
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        alumno = new Alumno();
        alumno.setId(1L);
        alumno.setNombre("Martín");
        alumno.setApellido("Fernández");
    }

    /** Última cuota del alumno, que vence el día indicado (la situación de pago se calcula con esa fecha). */
    private Cuota cuota(Long id, LocalDate vencimiento) {
        Membresia membresia = new Membresia(7L, "HIIT", 25000.0, 3, null);
        Inscripcion inscripcion = new Inscripcion(5L, vencimiento.minusDays(60), null,
                EstadoInscripcion.ACTIVA, alumno, membresia);
        Cuota cuota = inscripcion.agregarCuota(vencimiento.minusDays(ReglasCuota.DIAS_CUOTA));
        cuota.setId(id);
        return cuota;
    }

    private List<NotificacionResponse> notificacionesCon(Cuota ultima) {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscarUltimaCuota(1L)).thenReturn(Optional.ofNullable(ultima));
        return notificacionService.listarDelAlumno(EMAIL);
    }

    @Test
    void proxima_a_vencer_en_5_dias_notifica_que_esta_por_vencer() {
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, hoy.plusDays(5)));

        assertEquals(1, resultado.size());
        NotificacionResponse notificacion = resultado.get(0);
        assertEquals(TipoNotificacion.CUOTA_POR_VENCER, notificacion.getTipo());
        assertEquals("cuota-10", notificacion.getId());
        assertEquals(10L, notificacion.getCuotaId());
        assertEquals(hoy.plusDays(5), notificacion.getFechaVencimiento());
    }

    @Test
    void cuota_que_vence_hoy_dice_hoy() {
        NotificacionResponse notificacion = notificacionesCon(cuota(10L, hoy)).get(0);

        assertEquals(TipoNotificacion.CUOTA_POR_VENCER, notificacion.getTipo());
        assertTrue(notificacion.getMensaje().contains("vence hoy"));
    }

    @Test
    void al_dia_con_mas_de_5_dias_no_notifica() {
        assertTrue(notificacionesCon(cuota(10L, hoy.plusDays(6))).isEmpty());
    }

    @Test
    void vencida_notifica_que_esta_vencida() {
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, hoy.minusDays(1)));

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.CUOTA_VENCIDA, resultado.get(0).getTipo());
    }

    @Test
    void en_mora_sigue_notificando_vencida_sin_tipo_nuevo() {
        // El tipo CUOTA_EN_MORA queda para la rama de notificaciones (sección 15 del rediseño)
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, hoy.minusDays(60)));

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.CUOTA_VENCIDA, resultado.get(0).getTipo());
    }

    @Test
    void la_notificacion_desaparece_al_renovar() {
        Cuota vencida = cuota(10L, hoy.minusDays(3));
        assertEquals(1, notificacionesCon(vencida).size());

        // Al renovar, la última cuota pasa a ser la nueva (vence en 30 días)
        Cuota nueva = vencida.getInscripcion().agregarCuota(hoy);
        nueva.setId(11L);
        when(cuotaRepository.buscarUltimaCuota(1L)).thenReturn(Optional.of(nueva));

        assertTrue(notificacionService.listarDelAlumno(EMAIL).isEmpty());
    }

    @Test
    void sin_cuotas_no_notifica() {
        assertTrue(notificacionesCon(null).isEmpty());
    }

    @Test
    void no_se_duplica_la_notificacion_para_la_misma_cuota() {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscarUltimaCuota(1L)).thenReturn(Optional.of(cuota(10L, hoy.minusDays(3))));

        // Consultas repetidas (cada vez que el alumno entra) devuelven siempre la misma, una sola vez
        List<NotificacionResponse> primera = notificacionService.listarDelAlumno(EMAIL);
        List<NotificacionResponse> segunda = notificacionService.listarDelAlumno(EMAIL);

        assertEquals(1, primera.size());
        assertEquals(1, segunda.size());
        assertEquals(primera.get(0).getId(), segunda.get(0).getId());
    }

    @Test
    void cuenta_sin_alumno_asociado_lanza_error() {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> notificacionService.listarDelAlumno(EMAIL));
    }

    @Test
    void muestraEnLaAppLosAvisosPersistidosPorCancelacionDeTurno() {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        AvisoTurno aviso = new AvisoTurno(alumno, 50L, 70L, hoy, hoy.plusDays(2),
                "Turno cancelado por el gimnasio", "Recibiste una clase extra.");
        aviso.setId(30L);
        when(avisoTurnoRepository.findByAlumnoIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(aviso));

        List<NotificacionResponse> resultado = notificacionService.listarDelAlumno(EMAIL);

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.TURNO_CANCELADO, resultado.get(0).getTipo());
        assertEquals("turno-30", resultado.get(0).getId());
        assertEquals(70L, resultado.get(0).getTurnoId());
        assertEquals(hoy.plusDays(2), resultado.get(0).getFechaTurno());
    }

    @Test
    void muestraEnLaAppElAvisoDeTurnoReactivado() {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        AvisoTurnoReactivado aviso = new AvisoTurnoReactivado(alumno, 50L, 70L, hoy, hoy.plusDays(2),
                "Turno disponible nuevamente", "El turno volvió a estar disponible.");
        aviso.setId(31L);
        when(avisoTurnoReactivadoRepository.findByAlumnoIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(aviso));

        List<NotificacionResponse> resultado = notificacionService.listarDelAlumno(EMAIL);

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.TURNO_REACTIVADO, resultado.get(0).getTipo());
        assertEquals("turno-reactivado-31", resultado.get(0).getId());
        assertEquals(70L, resultado.get(0).getTurnoId());
        assertEquals(hoy.plusDays(2), resultado.get(0).getFechaTurno());
    }
}
