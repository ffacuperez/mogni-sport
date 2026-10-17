package mogni.system_backend.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;
import mogni.system_backend.turno.service.ConfiguracionGimnasioService;
import mogni.system_backend.turno.service.GeneracionTurnosService;
import mogni.system_backend.turno.service.InsercionTurnosService;

@ExtendWith(MockitoExtension.class)
class GeneracionTurnosServiceTest {

    /** Un lunes. */
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private FranjaHorariaRepository franjaHorariaRepository;

    @Mock
    private ConfiguracionGimnasioService configuracionGimnasioService;

    @Mock
    private InsercionTurnosService insercionTurnosService;

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

    // HU-259 prueba 21 y criterio 20: si la fecha ya tiene turnos no se genera ni se modifica nada.
    // Por eso modificar/eliminar una franja o cambiar el cupo no cambia los turnos ya generados (HU-252 pruebas 11, 13, 17)
    @Test
    void conTurnosYaGeneradosLosDevuelveSinTocarlos() {
        List<Turno> existentes = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.DISPONIBLE),
                turnoGenerado(2L, "09:00", 5, EstadoTurno.DISPONIBLE));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(existentes);

        List<Turno> turnos = generacionTurnosService.obtenerOGenerarTurnos(LUNES);

        assertSame(existentes, turnos);
        assertEquals(5, turnos.get(0).getCupoMaximo());
        // Ni se miran las franjas ni el cupo actual
        verifyNoInteractions(franjaHorariaRepository, configuracionGimnasioService, insercionTurnosService);
    }

    // HU-259 prueba 23: una fecha con todos sus turnos cancelados ya está generada
    @Test
    void conTodosLosTurnosCanceladosNoGeneraNuevos() {
        List<Turno> cancelados = List.of(turnoGenerado(1L, "08:00", 5, EstadoTurno.CANCELADO));
        when(turnoRepository.findByFechaOrderByHoraInicioAsc(LUNES)).thenReturn(cancelados);

        assertSame(cancelados, generacionTurnosService.obtenerOGenerarTurnos(LUNES));
        verifyNoInteractions(insercionTurnosService);
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
