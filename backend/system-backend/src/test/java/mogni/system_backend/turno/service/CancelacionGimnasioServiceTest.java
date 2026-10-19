package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.notificacion.entity.AvisoTurno;
import mogni.system_backend.notificacion.repository.AvisoTurnoRepository;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

@ExtendWith(MockitoExtension.class)
class CancelacionGimnasioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 13);
    private static final LocalDateTime AHORA = HOY.atTime(10, 30);

    @Mock private TurnoRepository turnoRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private ClaseExtraRepository claseExtraRepository;
    @Mock private AvisoTurnoRepository avisoTurnoRepository;
    @Mock private GeneracionTurnosService generacionTurnosService;

    private final List<Reserva> reservas = new ArrayList<>();
    private final List<ClaseExtra> clasesExtra = new ArrayList<>();
    private final List<AvisoTurno> avisos = new ArrayList<>();
    private CancelacionGimnasioService service;
    private long siguienteId = 100;

    @BeforeEach
    void setUp() {
        service = new CancelacionGimnasioService(
                turnoRepository, reservaRepository, claseExtraRepository, avisoTurnoRepository, generacionTurnosService);
        lenient().when(turnoRepository.findByIdConBloqueo(anyLong())).thenAnswer(inv ->
                todosLosTurnos().stream().filter(turno -> turno.getId().equals(inv.<Long>getArgument(0))).findFirst());
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
        lenient().when(reservaRepository.findByTurnoIdInAndEstado(any(), any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            EstadoReserva estado = inv.getArgument(1);
            return reservas.stream()
                    .filter(reserva -> ids.contains(reserva.getTurno().getId()) && reserva.getEstado() == estado)
                    .toList();
        });
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

    private List<Turno> todosLosTurnos() {
        return turnos;
    }

    private final List<Turno> turnos = new ArrayList<>();

    private Turno turno(LocalDate fecha, String hora) {
        LocalTime inicio = LocalTime.parse(hora);
        Turno turno = new Turno(siguienteId++, fecha, inicio, inicio.plusHours(1), 10, EstadoTurno.DISPONIBLE);
        turnos.add(turno);
        return turno;
    }

    private void reservar(Alumno alumno, Turno turno) {
        reservas.add(new Reserva(siguienteId++, alumno, turno, AHORA.minusDays(1), EstadoReserva.RESERVADA));
    }

    @Test
    void cancelaTurnoDeHoyQueTodaviaNoEmpezoYCompensaCadaReservaActiva() {
        Turno turno = turno(HOY, "11:30");
        Alumno primera = new Alumno();
        primera.setId(1L);
        Alumno segunda = new Alumno();
        segunda.setId(2L);
        reservar(primera, turno);
        reservar(segunda, turno);
        when(generacionTurnosService.obtenerOGenerarTurnos(HOY)).thenReturn(List.of(turno));

        var resultado = service.cancelarTurno(HOY, "11:30", AHORA);

        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        // La cancelación del propietario es manual: la reconciliación de franjas nunca la revierte
        assertEquals(mogni.system_backend.turno.entity.MotivoCancelacionTurno.MANUAL, turno.getMotivoCancelacion());
        assertEquals(2, resultado.getReservasCanceladas());
        assertEquals(2, resultado.getClasesExtraOtorgadas());
        assertTrue(reservas.stream().allMatch(r -> r.getEstado() == EstadoReserva.CANCELADA_POR_GIMNASIO));
        assertEquals(List.of(1L, 2L), clasesExtra.stream().map(c -> c.getAlumno().getId()).toList());
        assertTrue(clasesExtra.stream().allMatch(c -> c.getVigenteDesde().equals(LocalDate.of(2026, 10, 12))
                && c.getVigenteHasta().equals(LocalDate.of(2026, 10, 25))));
        assertEquals(2, avisos.size());
    }

    @Test
    void cancelaTodasLasFranjasFuturasDelDiaAunqueNoTenganReservas() {
        LocalDate fecha = HOY.plusDays(10);
        Turno primera = turno(fecha, "08:00");
        Turno segunda = turno(fecha, "09:00");
        when(generacionTurnosService.obtenerOGenerarTurnos(fecha)).thenReturn(List.of(primera, segunda));

        var resultado = service.cancelarDia(fecha, AHORA);

        assertEquals(2, resultado.getTurnosCancelados());
        assertEquals(EstadoTurno.CANCELADO, primera.getEstado());
        assertEquals(EstadoTurno.CANCELADO, segunda.getEstado());
        assertEquals(0, resultado.getReservasCanceladas());
        assertEquals(0, resultado.getClasesExtraOtorgadas());
    }

    @Test
    void permiteCancelarTurnoFuturoSinReservas() {
        LocalDate fecha = HOY.plusDays(2);
        Turno turno = turno(fecha, "12:00");
        when(generacionTurnosService.obtenerOGenerarTurnos(fecha)).thenReturn(List.of(turno));

        assertEquals(1, service.cancelarTurno(fecha, "12:00", AHORA).getTurnosCancelados());
        assertTrue(clasesExtra.isEmpty());
    }

    @Test
    void rechazaCancelarUnTurnoQueYaEmpezo() {
        Turno turno = turno(HOY, "10:00");
        when(generacionTurnosService.obtenerOGenerarTurnos(HOY)).thenReturn(List.of(turno));

        assertThrows(ConflictoException.class, () -> service.cancelarTurno(HOY, "10:00", AHORA));
        assertEquals(EstadoTurno.DISPONIBLE, turno.getEstado());
    }

    @Test
    void laCancelacionSoloAfectaLaFechaYFranjaIndicadas() {
        Turno turnoCancelado = turno(HOY.plusDays(2), "17:00");
        Turno otraSemana = turno(HOY.plusDays(9), "17:00");
        when(generacionTurnosService.obtenerOGenerarTurnos(turnoCancelado.getFecha())).thenReturn(List.of(turnoCancelado));

        service.cancelarTurno(turnoCancelado.getFecha(), "17:00", AHORA);

        assertEquals(EstadoTurno.CANCELADO, turnoCancelado.getEstado());
        assertEquals(EstadoTurno.DISPONIBLE, otraSemana.getEstado());
        verify(turnoRepository).save(turnoCancelado);
    }

    @Test
    void soloCompensaYNotificaAlAlumnoReservadoEnElTurno() {
        Turno turno = turno(HOY.plusDays(2), "11:00");
        Alumno afectado = new Alumno();
        afectado.setId(11L);
        reservar(afectado, turno);
        when(generacionTurnosService.obtenerOGenerarTurnos(turno.getFecha())).thenReturn(List.of(turno));

        service.cancelarTurno(turno.getFecha(), "11:00", AHORA);

        assertEquals(List.of(11L), clasesExtra.stream().map(c -> c.getAlumno().getId()).toList());
        assertEquals(List.of(11L), avisos.stream().map(a -> a.getAlumno().getId()).toList());
        assertEquals(EstadoReserva.CANCELADA_POR_GIMNASIO, reservas.get(0).getEstado());
    }

    @Test
    void elResumenAntesDeConfirmarCuentaLasReservasActivasDelTurno() {
        Turno turno = turno(HOY.plusDays(2), "11:00");
        Alumno alumno = new Alumno();
        reservar(alumno, turno);
        reservas.add(new Reserva(siguienteId++, alumno, turno, AHORA.minusDays(1), EstadoReserva.CANCELADA));
        when(generacionTurnosService.obtenerOGenerarTurnos(turno.getFecha())).thenReturn(List.of(turno));

        var resumen = service.resumenTurno(turno.getFecha(), "11:00", AHORA);

        assertEquals(1, resumen.getTurnosAfectados());
        assertEquals(1, resumen.getReservasAfectadas());
    }

    @Test
    void elResumenDelDiaOmiteFranjasPasadasYCanceladas() {
        LocalDate fecha = HOY;
        Turno pasado = turno(fecha, "09:00");
        Turno vigente = turno(fecha, "11:00");
        Turno cancelado = turno(fecha, "12:00");
        cancelado.setEstado(EstadoTurno.CANCELADO);
        Alumno alumno = new Alumno();
        reservar(alumno, pasado);
        reservar(alumno, vigente);
        reservar(alumno, cancelado);
        when(generacionTurnosService.obtenerOGenerarTurnos(fecha)).thenReturn(List.of(pasado, vigente, cancelado));

        var resumen = service.resumenDia(fecha, AHORA);

        assertEquals(1, resumen.getTurnosAfectados());
        assertEquals(1, resumen.getReservasAfectadas());
    }
}
