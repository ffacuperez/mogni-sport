package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.persistence.EntityManager;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.notificacion.entity.AvisoTurno;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.MotivoCancelacionTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.service.ReconciliacionTurnosService.Plan;
import mogni.system_backend.turno.service.ReconciliacionTurnosService.Resultado;

@ExtendWith(MockitoExtension.class)
class ReconciliacionTurnosServiceTest {

    /** Un martes. */
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);
    /** Ningún turno del martes empezó todavía. */
    private static final LocalDateTime AHORA = MARTES.atTime(7, 0);
    private static final int CUPO_VIGENTE = 8;

    @Mock private TurnoRepository turnoRepository;
    @Mock private FranjaHorariaRepository franjaHorariaRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private ConfiguracionGimnasioService configuracionGimnasioService;
    @Mock private ClaseExtraRepository claseExtraRepository;
    @Mock private AvisoTurnoRepository avisoTurnoRepository;
    @Mock private GeneracionTurnosService generacionTurnosService;
    @Mock private EntityManager entityManager;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final List<Turno> turnos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final List<ClaseExtra> clasesExtra = new ArrayList<>();
    private final List<AvisoTurno> avisos = new ArrayList<>();
    private long siguienteId = 100;
    private ReconciliacionTurnosService service;

    @BeforeEach
    void setUp() {
        CancelacionGimnasioService cancelacion = new CancelacionGimnasioService(
                turnoRepository, reservaRepository, claseExtraRepository, avisoTurnoRepository, generacionTurnosService,
                eventPublisher);
        service = new ReconciliacionTurnosService(turnoRepository, franjaHorariaRepository, reservaRepository,
                alumnoRepository, configuracionGimnasioService, cancelacion, entityManager);

        lenient().when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(CUPO_VIGENTE);
        lenient().when(turnoRepository.findByFechaOrderByHoraInicioAsc(MARTES)).thenAnswer(inv -> turnos.stream()
                .sorted((a, b) -> a.getHoraInicio().compareTo(b.getHoraInicio())).toList());
        lenient().when(turnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv -> turnos.stream()
                .filter(turno -> turno.getId().equals(inv.<Long>getArgument(0))).findFirst());
        lenient().when(reservaRepository.findWithAlumnoByTurnoIdInAndEstado(any(), any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            EstadoReserva estado = inv.getArgument(1);
            return reservas.stream()
                    .filter(reserva -> ids.contains(reserva.getTurno().getId()) && reserva.getEstado() == estado)
                    .toList();
        });
        lenient().when(reservaRepository.countByTurnoIdAndEstado(anyLong(), any())).thenAnswer(inv ->
                reservas.stream().filter(reserva -> reserva.getTurno().getId().equals(inv.<Long>getArgument(0))
                        && reserva.getEstado() == inv.getArgument(1)).count());
        lenient().when(claseExtraRepository.save(any(ClaseExtra.class))).thenAnswer(inv -> {
            ClaseExtra clase = inv.getArgument(0);
            clase.setId(siguienteId++);
            clasesExtra.add(clase);
            return clase;
        });
        lenient().when(avisoTurnoRepository.save(any(AvisoTurno.class))).thenAnswer(inv -> {
            AvisoTurno aviso = inv.getArgument(0);
            aviso.setId(siguienteId++);
            avisos.add(aviso);
            return aviso;
        });
    }

    private static LocalTime h(String hora) {
        return LocalTime.parse(hora);
    }

    private static FranjaHoraria franja(String inicio, String fin) {
        return new FranjaHoraria(1L, DayOfWeek.TUESDAY, h(inicio), h(fin));
    }

    private void franjasVigentes(FranjaHoraria... franjas) {
        lenient().when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.TUESDAY))
                .thenReturn(List.of(franjas));
    }

    private Turno turno(String hora, int cupo, EstadoTurno estado, MotivoCancelacionTurno motivo) {
        LocalTime inicio = h(hora);
        Turno turno = new Turno(siguienteId++, MARTES, inicio, inicio.plusHours(1), cupo, estado, motivo);
        turnos.add(turno);
        return turno;
    }

    private Turno turno(String hora) {
        return turno(hora, 5, EstadoTurno.DISPONIBLE, null);
    }

    private Alumno alumno(long id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        return alumno;
    }

    private Reserva reservar(Alumno alumno, Turno turno) {
        Reserva reserva = new Reserva(siguienteId++, alumno, turno, AHORA.minusDays(1), EstadoReserva.RESERVADA);
        reservas.add(reserva);
        return reserva;
    }

    // ---------- Plan ----------

    @Test
    void plan_conLosTurnosAlDiaEstaVacio() {
        turno("08:00");
        turno("09:00");

        Plan plan = ReconciliacionTurnosService.calcularPlan(MARTES, turnos, List.of(franja("08:00", "10:00")), AHORA);

        assertTrue(plan.estaVacio());
    }

    @Test
    void plan_laFranjaSeAgrandoCreaLosTurnosQueFaltan() {
        turno("08:00");

        Plan plan = ReconciliacionTurnosService.calcularPlan(MARTES, turnos, List.of(franja("08:00", "11:00")), AHORA);

        assertEquals(List.of(h("09:00"), h("10:00")),
                plan.aCrear().stream().map(horario -> horario.horaInicio()).toList());
        assertTrue(plan.aCancelar().isEmpty());
    }

    @Test
    void plan_noCreaTurnosQueYaEmpezaron() {
        turno("10:00");

        Plan plan = ReconciliacionTurnosService.calcularPlan(MARTES, turnos, List.of(franja("08:00", "11:00")),
                MARTES.atTime(9, 30));

        // 08:00 ya pasó, 09:00 ya empezó y 10:00 ya existe
        assertTrue(plan.estaVacio());
    }

    @Test
    void plan_unTurnoSinFranjaSeCancelaPeroUnoYaCanceladoNo() {
        Turno huerfano = turno("08:00");
        turno("09:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.MANUAL);
        turno("10:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.FRANJA_ELIMINADA);

        Plan plan = ReconciliacionTurnosService.calcularPlan(MARTES, turnos, List.of(), AHORA);

        assertEquals(List.of(huerfano), plan.aCancelar());
        assertTrue(plan.aReactivar().isEmpty());
    }

    @Test
    void plan_soloSeReactivanLosCanceladosPorFranja() {
        turno("08:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.MANUAL);
        turno("09:00", 5, EstadoTurno.CANCELADO, null);
        Turno porFranja = turno("10:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.FRANJA_ELIMINADA);

        Plan plan = ReconciliacionTurnosService.calcularPlan(MARTES, turnos, List.of(franja("08:00", "11:00")), AHORA);

        assertEquals(List.of(porFranja), plan.aReactivar());
        assertTrue(plan.aCrear().isEmpty());
        assertTrue(plan.aCancelar().isEmpty());
    }

    // ---------- reconciliarFecha ----------

    @Test
    void laFranjaSeAgrandaCreaLosTurnosNuevosConElCupoVigente() {
        turno("08:00");
        franjasVigentes(franja("08:00", "10:00"));

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Turno>> captor = ArgumentCaptor.forClass(List.class);
        verify(turnoRepository).saveAllAndFlush(captor.capture());
        List<Turno> nuevos = captor.getValue();
        assertEquals(1, nuevos.size());
        assertEquals(h("09:00"), nuevos.get(0).getHoraInicio());
        assertEquals(h("10:00"), nuevos.get(0).getHoraFin());
        assertEquals(CUPO_VIGENTE, nuevos.get(0).getCupoMaximo());
        assertEquals(EstadoTurno.DISPONIBLE, nuevos.get(0).getEstado());
        assertEquals(1, resultado.creados());
    }

    @Test
    void unTurnoSinFranjaYSinReservasPasaACanceladoPorFranja() {
        turno("08:00");
        Turno huerfano = turno("09:00");
        franjasVigentes(franja("08:00", "09:00"));

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertEquals(EstadoTurno.CANCELADO, huerfano.getEstado());
        assertEquals(MotivoCancelacionTurno.FRANJA_ELIMINADA, huerfano.getMotivoCancelacion());
        assertEquals(1, resultado.cancelados());
        assertEquals(0, resultado.reservasCanceladas());
        assertTrue(clasesExtra.isEmpty());
        assertTrue(avisos.isEmpty());
        verify(turnoRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void unTurnoSinFranjaConReservasUsaLaCancelacionDelGimnasio() {
        Turno huerfano = turno("09:00");
        Reserva deLaSegunda = reservar(alumno(2L), huerfano);
        Reserva deLaPrimera = reservar(alumno(1L), huerfano);
        franjasVigentes();

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertEquals(EstadoTurno.CANCELADO, huerfano.getEstado());
        assertEquals(MotivoCancelacionTurno.FRANJA_ELIMINADA, huerfano.getMotivoCancelacion());
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, deLaPrimera.getEstado());
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, deLaSegunda.getEstado());
        assertEquals(2, resultado.reservasCanceladas());
        assertEquals(2, clasesExtra.size());
        assertEquals(2, avisos.size());
    }

    @Test
    void bloqueaPrimeroLosAlumnosPorIdYDespuesElTurno() {
        Turno huerfano = turno("09:00");
        reservar(alumno(7L), huerfano);
        reservar(alumno(3L), huerfano);
        franjasVigentes();

        service.reconciliarFecha(MARTES, AHORA);

        // Mismo orden que ReservaService (alumno y después turno), así no se traban entre sí
        InOrder orden = inOrder(alumnoRepository, turnoRepository);
        orden.verify(alumnoRepository).findByIdConBloqueo(3L);
        orden.verify(alumnoRepository).findByIdConBloqueo(7L);
        orden.verify(turnoRepository).findByIdConBloqueo(huerfano.getId());
    }

    @Test
    void noTocaLosTurnosQueYaEmpezaron() {
        Turno empezado = turno("08:00");
        reservar(alumno(1L), empezado);
        franjasVigentes();

        Resultado resultado = service.reconciliarFecha(MARTES, MARTES.atTime(8, 15));

        assertEquals(EstadoTurno.DISPONIBLE, empezado.getEstado());
        assertFalse(resultado.huboCambios());
        assertTrue(clasesExtra.isEmpty());
    }


    @Test
    void noRegeneraNiReactivaLosTurnosCanceladosAPropositoDelGimnasio() {
        Turno manual = turno("08:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.MANUAL);
        Turno anterior = turno("09:00", 5, EstadoTurno.CANCELADO, null);
        franjasVigentes(franja("08:00", "10:00"));

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertEquals(EstadoTurno.CANCELADO, manual.getEstado());
        assertEquals(EstadoTurno.CANCELADO, anterior.getEstado());
        assertEquals(MotivoCancelacionTurno.MANUAL, manual.getMotivoCancelacion());
        assertFalse(resultado.huboCambios());
        verify(turnoRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void unTurnoCanceladoPorFranjaSeReactivaSiLaFranjaVuelveYConservaSusReservasCanceladas() {
        Turno turno = turno("08:00", 3, EstadoTurno.CANCELADO, MotivoCancelacionTurno.FRANJA_ELIMINADA);
        Reserva vieja = reservar(alumno(1L), turno);
        vieja.setEstado(EstadoReserva.CANCELADA_POR_GIMNASIO);
        franjasVigentes(franja("08:00", "09:00"));

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
        assertNull(turno.getMotivoCancelacion());
        assertEquals(CUPO_VIGENTE, turno.getCupoMaximo());
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, vieja.getEstado());
        assertEquals(1, resultado.reactivados());
    }

    @Test
    void unaFechaSinTurnosNoSeToca() {
        franjasVigentes(franja("08:00", "10:00"));

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertFalse(resultado.huboCambios());
        verify(turnoRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void siAlBloquearYaEstabaCanceladoNoLoCancelaDeNuevo() {
        Turno huerfano = turno("09:00");
        franjasVigentes();
        // Otro proceso lo canceló entre que se leyó y se bloqueó
        when(turnoRepository.findByIdConBloqueo(huerfano.getId())).thenAnswer(inv -> {
            huerfano.cancelar(MotivoCancelacionTurno.MANUAL);
            return Optional.of(huerfano);
        });

        Resultado resultado = service.reconciliarFecha(MARTES, AHORA);

        assertEquals(0, resultado.cancelados());
        assertEquals(MotivoCancelacionTurno.MANUAL, huerfano.getMotivoCancelacion());
    }

    // ---------- Cupo ----------

    @Test
    void elCupoNuevoSeAplicaALosTurnosFuturosSinReservas() {
        Turno sinReservas = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);

        int actualizados = service.aplicarCupoEnFecha(MARTES, 12, AHORA);

        assertEquals(12, sinReservas.getCupoMaximo());
        assertEquals(1, actualizados);
        verify(turnoRepository).save(sinReservas);
    }

    @Test
    void losTurnosConReservasConservanElCupoAnterior() {
        Turno conReservas = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        reservar(alumno(1L), conReservas);

        int actualizados = service.aplicarCupoEnFecha(MARTES, 12, AHORA);

        assertEquals(5, conReservas.getCupoMaximo());
        assertEquals(0, actualizados);
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void unaReservaCanceladaNoImpideAplicarElCupoNuevo() {
        Turno turno = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        reservar(alumno(1L), turno).setEstado(EstadoReserva.CANCELADA);

        assertEquals(1, service.aplicarCupoEnFecha(MARTES, 12, AHORA));
        assertEquals(12, turno.getCupoMaximo());
    }

    @Test
    void elCupoNoCambiaEnTurnosIniciadosNiCancelados() {
        Turno iniciado = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        Turno cancelado = turno("10:00", 5, EstadoTurno.CANCELADO, MotivoCancelacionTurno.MANUAL);

        int actualizados = service.aplicarCupoEnFecha(MARTES, 12, MARTES.atTime(8, 30));

        assertEquals(5, iniciado.getCupoMaximo());
        assertEquals(5, cancelado.getCupoMaximo());
        assertEquals(0, actualizados);
    }

    @Test
    void elCupoSoloCambiaEnLosTurnosCuyaHoraDeInicioEsPosteriorAAhora() {
        Turno yaPaso = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        Turno enCurso = turno("14:00", 5, EstadoTurno.DISPONIBLE, null);
        Turno empiezaJustoAhora = turno("15:00", 5, EstadoTurno.DISPONIBLE, null);
        Turno proximo = turno("15:01", 5, EstadoTurno.DISPONIBLE, null);
        Turno masTarde = turno("18:00", 5, EstadoTurno.DISPONIBLE, null);

        int actualizados = service.aplicarCupoEnFecha(MARTES, 12, MARTES.atTime(15, 0));

        assertEquals(5, yaPaso.getCupoMaximo());
        assertEquals(5, enCurso.getCupoMaximo());
        // Un turno que empieza exactamente ahora ya empezó
        assertEquals(5, empiezaJustoAhora.getCupoMaximo());
        assertEquals(12, proximo.getCupoMaximo());
        assertEquals(12, masTarde.getCupoMaximo());
        assertEquals(2, actualizados);
        verify(turnoRepository, never()).save(yaPaso);
        verify(turnoRepository, never()).save(enCurso);
        verify(turnoRepository, never()).save(empiezaJustoAhora);
    }

    @Test
    void elCupoNoCambiaEnNingunTurnoDeUnaFechaQueYaPaso() {
        Turno manana = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        Turno tarde = turno("20:00", 5, EstadoTurno.DISPONIBLE, null);

        int actualizados = service.aplicarCupoEnFecha(MARTES, 12, MARTES.plusDays(1).atTime(7, 0));

        assertEquals(5, manana.getCupoMaximo());
        assertEquals(5, tarde.getCupoMaximo());
        assertEquals(0, actualizados);
        verify(turnoRepository, never()).findByIdConBloqueo(anyLong());
    }

    @Test
    void recuentaLasReservasConElTurnoBloqueado() {
        Turno turno = turno("08:00", 5, EstadoTurno.DISPONIBLE, null);
        // Entra una reserva entre que se eligió el turno y se lo bloqueó
        when(turnoRepository.findByIdConBloqueo(turno.getId())).thenAnswer(inv -> {
            reservar(alumno(1L), turno);
            return Optional.of(turno);
        });

        assertEquals(0, service.aplicarCupoEnFecha(MARTES, 12, AHORA));
        assertEquals(5, turno.getCupoMaximo());
    }
}
