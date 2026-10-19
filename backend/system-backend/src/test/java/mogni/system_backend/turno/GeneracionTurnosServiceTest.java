package mogni.system_backend.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;

import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.service.ConfiguracionGimnasioService;
import mogni.system_backend.turno.service.GeneracionTurnosService;
import mogni.system_backend.turno.service.InsercionTurnosService;
import mogni.system_backend.turno.service.ReconciliacionTurnosService;

@ExtendWith(MockitoExtension.class)
class GeneracionTurnosServiceTest {

    /** Un lunes. */
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);

    /** Ningún turno del lunes empezó todavía. */
    private static final LocalDateTime ANTES_DEL_LUNES = LUNES.atTime(7, 0);

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private FranjaHorariaRepository franjaHorariaRepository;

    @Mock
    private ConfiguracionGimnasioService configuracionGimnasioService;

    @Mock
    private InsercionTurnosService insercionTurnosService;

    @Mock
    private ReconciliacionTurnosService reconciliacionTurnosService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private GeneracionTurnosService generacionTurnosService;

    private static LocalTime h(String hora) {
        return LocalTime.parse(hora);
    }

    private static Turno turnoGenerado(Long id, String inicio, int cupo, EstadoTurno estado) {
        return new Turno(id, LUNES, h(inicio), h(inicio).plusHours(1), cupo, estado);
    }

    @SuppressWarnings("unchecked")
    private List<Turno> turnosInsertados() {
        ArgumentCaptor<List<Turno>> captor = ArgumentCaptor.forClass(List.class);
        verify(insercionTurnosService).insertar(captor.capture());
        return captor.getValue();
    }

    // HU-259 prueba 20: el lunes con la franja de 8 a 12 genera 4 turnos de 1 hora con el cupo vigente
    @Test
    void sinTurnosGeneraLosDelDiaConElCupoVigente() {
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(List.of());
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("12:00"))));
        when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(7);
        when(insercionTurnosService.insertar(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(LUNES);

        assertEquals(4, turnos.size());
        List<Turno> insertados = turnosInsertados();
        assertEquals(List.of(h("08:00"), h("09:00"), h("10:00"), h("11:00")),
                insertados.stream().map(Turno::getHoraInicio).toList());
        assertEquals(h("12:00"), insertados.get(3).getHoraFin());
        assertTrue(insertados.stream().allMatch(t -> t.getFecha().equals(LUNES)));
        assertTrue(insertados.stream().allMatch(t -> t.getCupoMaximo() == 7));
        assertTrue(insertados.stream().allMatch(t -> t.getEstado() == EstadoTurno.DISPONIBLE));
    }

    @Test
    void conVariasFranjasGeneraLosTurnosDeTodasEnOrden() {
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(List.of());
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of(
                new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("10:00")),
                new FranjaHoraria(2L, DayOfWeek.MONDAY, h("16:30"), h("18:30"))));
        when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(5);
        when(insercionTurnosService.insertar(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(LUNES);

        assertEquals(List.of(h("08:00"), h("09:00"), h("16:30"), h("17:30")),
                turnos.stream().map(Turno::getHoraInicio).toList());
    }

    // HU-259 prueba 21: si la fecha ya tiene turnos y coinciden con las franjas, no se genera ni se modifica nada
    @Test
    void conTurnosQueCoincidenConLasFranjasLosDevuelveSinTocarlos() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE),
                turnoGenerado(2L, "09:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("10:00"))));

        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(LUNES, ANTES_DEL_LUNES);

        assertSame(existentes, turnos);
        assertEquals(5, turnos.get(0).getCupoMaximo());
        verifyNoInteractions(reconciliacionTurnosService, insercionTurnosService);
    }

    // HU-259 prueba 23: una fecha con todos sus turnos cancelados ya está generada
    @Test
    void conTodosLosTurnosCanceladosNoGeneraNuevos() {
        List<Turno> cancelados = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.CANCELADO));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(cancelados);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("09:00"))));

        assertSame(cancelados, generacionTurnosService.obtenerOGenerarTurnos(LUNES, ANTES_DEL_LUNES));
        verifyNoInteractions(insercionTurnosService, reconciliacionTurnosService);
    }

    // Red de seguridad: la franja se agrandó y la reconciliación al editar no corrió
    @Test
    void siFaltanTurnosDeLasFranjasReconcilia() {
        List<Turno> viejos = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        List<Turno> reconciliados = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE),
                turnoGenerado(2L, "09:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(viejos, reconciliados);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("10:00"))));

        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(LUNES, ANTES_DEL_LUNES);

        assertSame(reconciliados, turnos);
        verify(reconciliacionTurnosService).reconciliarFecha(LUNES, ANTES_DEL_LUNES);
        // Se sueltan las copias viejas para que la relectura traiga el estado confirmado por la otra transacción
        verify(entityManager).detach(viejos.get(0));
    }

    // Red de seguridad: un turno que ya no tiene franja también dispara la reconciliación
    @Test
    void siUnTurnoYaNoTieneFranjaReconcilia() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of());

        generacionTurnosService.obtenerOGenerarTurnos(LUNES, ANTES_DEL_LUNES);

        verify(reconciliacionTurnosService).reconciliarFecha(LUNES, ANTES_DEL_LUNES);
    }

    // Si la reconciliación falla, la consulta del alumno sigue funcionando con lo que había
    @Test
    void siLaReconciliacionFallaDevuelveLosTurnosExistentes() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of());
        doThrow(new IllegalStateException("deadlock")).when(reconciliacionTurnosService)
                .reconciliarFecha(LUNES, ANTES_DEL_LUNES);

        assertSame(existentes, generacionTurnosService.obtenerOGenerarTurnos(LUNES, ANTES_DEL_LUNES));
        verifyNoInteractions(entityManager);
    }

    // Las fechas pasadas no se tocan: ni se miran las franjas
    @Test
    void conUnaFechaPasadaNoReconcilia() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);

        assertSame(existentes, generacionTurnosService.obtenerOGenerarTurnos(LUNES, LUNES.plusDays(1).atTime(8, 0)));
        verifyNoInteractions(franjaHorariaRepository, reconciliacionTurnosService);
    }

    // Un turno de hoy que ya empezó no se reconcilia aunque no coincida con las franjas
    @Test
    void conTurnosYaIniciadosNoReconcilia() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of());

        assertSame(existentes, generacionTurnosService.obtenerOGenerarTurnos(LUNES, LUNES.atTime(8, 30)));
        verifyNoInteractions(reconciliacionTurnosService);
    }

    // HU-259 prueba 22 y HU-252 prueba 12: un día sin franjas no genera turnos
    @Test
    void sinFranjasNoGeneraNada() {
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(List.of());
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of());

        assertTrue(generacionTurnosService.obtenerOGenerarTurnos(LUNES).isEmpty());
        verify(insercionTurnosService, never()).insertar(any());
    }

    // HU-252 prueba 18: los turnos generados después de cambiar el cupo usan el nuevo
    @Test
    void usaElCupoVigenteAlMomentoDeGenerar() {
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(List.of());
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("09:00"))));
        when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(20);
        when(insercionTurnosService.insertar(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(20, generacionTurnosService.obtenerOGenerarTurnos(LUNES).get(0).getCupoMaximo());
    }

    // Criterio 21: si otra consulta generó la misma fecha a la vez, la restricción única falla y se releen
    @Test
    void siOtraConsultaYaLosGeneroLosVuelveALeer() {
        List<Turno> deLaOtraConsulta = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(List.of(), deLaOtraConsulta);
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(new FranjaHoraria(1L, DayOfWeek.MONDAY, h("08:00"), h("09:00"))));
        when(configuracionGimnasioService.obtenerCupoMaximo()).thenReturn(5);
        when(insercionTurnosService.insertar(anyList()))
                .thenThrow(new DataIntegrityViolationException("uk_turnos_fecha_hora_inicio"));

        assertSame(deLaOtraConsulta, generacionTurnosService.obtenerOGenerarTurnos(LUNES));
    }
}
