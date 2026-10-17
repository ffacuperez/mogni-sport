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
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.dto.TipoNotificacion;
import mogni.system_backend.notificacion.entity.AvisoTurno;
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

    private Cuota cuota(Long id, EstadoCuota estado, LocalDate vencimiento) {
        Membresia membresia = new Membresia(7L, "HIIT", 25000.0, 3, null);
        Inscripcion inscripcion = new Inscripcion(5L, vencimiento.minusDays(Cuota.DIAS_VENCIMIENTO), null,
                EstadoInscripcion.ACTIVA, alumno, membresia);
        Cuota cuota = Cuota.primeraDe(inscripcion);
        cuota.setId(id);
        cuota.setEstado(estado);
        cuota.setFechaVencimiento(vencimiento);
        return cuota;
    }

    private List<NotificacionResponse> notificacionesCon(Cuota... cuotas) {
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, 1L)).thenReturn(List.of(cuotas));
        return notificacionService.listarDelAlumno(EMAIL);
    }

    @Test
    void cuota_pendiente_que_vence_en_5_dias_o_menos_notifica_que_esta_por_vencer() {
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, EstadoCuota.PENDIENTE, hoy.plusDays(5)));

        assertEquals(1, resultado.size());
        NotificacionResponse notificacion = resultado.get(0);
        assertEquals(TipoNotificacion.CUOTA_POR_VENCER, notificacion.getTipo());
        assertEquals("cuota-10", notificacion.getId());
        assertEquals(10L, notificacion.getCuotaId());
        assertEquals(hoy.plusDays(5), notificacion.getFechaVencimiento());
    }

    @Test
    void cuota_pendiente_que_vence_hoy_dice_hoy() {
        NotificacionResponse notificacion = notificacionesCon(cuota(10L, EstadoCuota.PENDIENTE, hoy)).get(0);

        assertEquals(TipoNotificacion.CUOTA_POR_VENCER, notificacion.getTipo());
        assertTrue(notificacion.getMensaje().contains("vence hoy"));
    }

    @Test
    void cuota_pendiente_que_vence_en_mas_de_5_dias_no_notifica() {
        assertTrue(notificacionesCon(cuota(10L, EstadoCuota.PENDIENTE, hoy.plusDays(6))).isEmpty());
    }

    @Test
    void cuota_atrasada_notifica_que_esta_vencida() {
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, EstadoCuota.ATRASADA, hoy.minusDays(1)));

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.CUOTA_VENCIDA, resultado.get(0).getTipo());
    }

    @Test
    void cuota_pendiente_con_vencimiento_pasado_notifica_vencida_aunque_la_tarea_diaria_no_haya_corrido() {
        NotificacionResponse notificacion = notificacionesCon(cuota(10L, EstadoCuota.PENDIENTE, hoy.minusDays(1))).get(0);

        assertEquals(TipoNotificacion.CUOTA_VENCIDA, notificacion.getTipo());
    }

    @Test
    void la_notificacion_persiste_mientras_la_cuota_no_esta_pagada() {
        // Vencida hace dos meses y sigue sin pagarse: la notificación sigue estando
        List<NotificacionResponse> resultado = notificacionesCon(cuota(10L, EstadoCuota.ATRASADA, hoy.minusDays(60)));

        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.CUOTA_VENCIDA, resultado.get(0).getTipo());
    }

    @Test
    void la_notificacion_desaparece_cuando_la_cuota_pasa_a_pagada() {
        Cuota cuota = cuota(10L, EstadoCuota.ATRASADA, hoy.minusDays(3));
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, 1L)).thenReturn(List.of(cuota));
        assertEquals(1, notificacionService.listarDelAlumno(EMAIL).size());

        cuota.setEstado(EstadoCuota.PAGADA); // lo que hace PagoService.registrarPago

        assertTrue(notificacionService.listarDelAlumno(EMAIL).isEmpty());
    }

    @Test
    void cuota_pagada_no_notifica_aunque_su_fecha_este_cerca_o_haya_pasado() {
        assertTrue(notificacionesCon(
                cuota(10L, EstadoCuota.PAGADA, hoy.plusDays(2)),
                cuota(11L, EstadoCuota.PAGADA, hoy.minusDays(10))).isEmpty());
    }

    @Test
    void no_se_duplica_la_notificacion_para_la_misma_cuota() {
        Cuota cuota = cuota(10L, EstadoCuota.ATRASADA, hoy.minusDays(3));
        when(alumnoRepository.findByCuentaEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(alumno));
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, 1L)).thenReturn(List.of(cuota));

        // Consultas repetidas (cada vez que el alumno entra) devuelven siempre la misma, una sola vez
        List<NotificacionResponse> primera = notificacionService.listarDelAlumno(EMAIL);
        List<NotificacionResponse> segunda = notificacionService.listarDelAlumno(EMAIL);

        assertEquals(1, primera.size());
        assertEquals(1, segunda.size());
        assertEquals(primera.get(0).getId(), segunda.get(0).getId());
    }

    @Test
    void varias_cuotas_impagas_ordenadas_de_la_mas_reciente_a_la_mas_antigua() {
        List<NotificacionResponse> resultado = notificacionesCon(
                cuota(10L, EstadoCuota.ATRASADA, hoy.minusDays(30)),
                cuota(11L, EstadoCuota.PENDIENTE, hoy.plusDays(3)));

        assertEquals(List.of("cuota-11", "cuota-10"), resultado.stream().map(NotificacionResponse::getId).toList());
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
}
