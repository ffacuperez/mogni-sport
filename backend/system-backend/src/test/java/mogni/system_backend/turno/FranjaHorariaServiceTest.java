package mogni.system_backend.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.FranjaHorariaRequest;
import mogni.system_backend.turno.dto.FranjaHorariaResponse;
import mogni.system_backend.turno.entity.FranjaHoraria;
import mogni.system_backend.turno.repository.FranjaHorariaRepository;
import mogni.system_backend.turno.service.FranjaHorariaService;

@ExtendWith(MockitoExtension.class)
class FranjaHorariaServiceTest {

    @Mock
    private FranjaHorariaRepository franjaHorariaRepository;

    @InjectMocks
    private FranjaHorariaService franjaHorariaService;

    private static LocalTime h(String hora) {
        return LocalTime.parse(hora);
    }

    private static FranjaHorariaRequest request(DayOfWeek dia, String inicio, String fin) {
        FranjaHorariaRequest request = new FranjaHorariaRequest();
        request.setDiaSemana(dia);
        request.setHoraInicio(h(inicio));
        request.setHoraFin(fin == null ? null : h(fin));
        return request;
    }

    private static FranjaHoraria franja(Long id, DayOfWeek dia, String inicio, String fin) {
        return new FranjaHoraria(id, dia, h(inicio), h(fin));
    }

    private void guardarDevuelveLoMismo() {
        when(franjaHorariaRepository.save(any(FranjaHoraria.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Prueba 1
    @Test
    void crearFranja_deOchoADoceMuestraCuatroTurnos() {
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of());
        guardarDevuelveLoMismo();

        FranjaHorariaResponse response = franjaHorariaService.crearFranja(request(DayOfWeek.MONDAY, "08:00", "12:00"));

        assertEquals(DayOfWeek.MONDAY, response.getDiaSemana());
        assertEquals("08:00", response.getHoraInicio());
        assertEquals("12:00", response.getHoraFin());
        assertEquals(4, response.getCantidadTurnos());
        assertEquals("08:00", response.getTurnos().get(0).getHoraInicio());
        assertEquals("09:00", response.getTurnos().get(0).getHoraFin());
        assertEquals("12:00", response.getTurnos().get(3).getHoraFin());
    }

    // Prueba 2: dos franjas no superpuestas el mismo día
    @Test
    void crearFranja_noSuperpuestaConOtraDelMismoDiaEsValida() {
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(franja(1L, DayOfWeek.MONDAY, "08:00", "12:00")));
        guardarDevuelveLoMismo();

        FranjaHorariaResponse response = franjaHorariaService.crearFranja(request(DayOfWeek.MONDAY, "16:00", "20:00"));

        assertEquals(4, response.getCantidadTurnos());
    }

    // Prueba 3
    @Test
    void crearFranja_dieciseisYMediaADiecinueveYMediaDaTresTurnos() {
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.TUESDAY)).thenReturn(List.of());
        guardarDevuelveLoMismo();

        FranjaHorariaResponse response = franjaHorariaService.crearFranja(request(DayOfWeek.TUESDAY, "16:30", "19:30"));

        assertEquals(3, response.getCantidadTurnos());
        assertEquals("16:30", response.getTurnos().get(0).getHoraInicio());
        assertEquals("17:30", response.getTurnos().get(1).getHoraInicio());
        assertEquals("19:30", response.getTurnos().get(2).getHoraFin());
    }

    // Prueba 4
    @Test
    void crearFranja_queNoEsMultiploDeUnaHoraFalla() {
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.crearFranja(request(DayOfWeek.MONDAY, "08:00", "12:30")));

        assertEquals("La franja tiene que durar horas completas (los turnos son de 1 hora).", ex.getMessage());
        verify(franjaHorariaRepository, never()).save(any());
    }

    // Prueba 5
    @Test
    void crearFranja_conFinAnteriorOIgualAlInicioFalla() {
        RegistroInvalidoException anterior = assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.crearFranja(request(DayOfWeek.SUNDAY, "16:00", "15:00")));
        RegistroInvalidoException igual = assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.crearFranja(request(DayOfWeek.SUNDAY, "16:00", "16:00")));

        assertEquals("La hora de fin debe ser posterior a la de inicio.", anterior.getMessage());
        assertEquals("La hora de fin debe ser posterior a la de inicio.", igual.getMessage());
        verify(franjaHorariaRepository, never()).save(any());
    }

    // Prueba 6
    @Test
    void crearFranja_superpuestaConOtraDelMismoDiaFalla() {
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.WEDNESDAY))
                .thenReturn(List.of(franja(1L, DayOfWeek.WEDNESDAY, "08:00", "12:00")));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.crearFranja(request(DayOfWeek.WEDNESDAY, "11:00", "13:00")));

        assertEquals("Esta franja se superpone con 08:00 – 12:00 del miércoles.", ex.getMessage());
        verify(franjaHorariaRepository, never()).save(any());
    }

    // Prueba 8
    @Test
    void crearFranja_contiguaConOtraDelMismoDiaEsValida() {
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(franja(1L, DayOfWeek.MONDAY, "08:00", "12:00")));
        guardarDevuelveLoMismo();

        FranjaHorariaResponse response = franjaHorariaService.crearFranja(request(DayOfWeek.MONDAY, "12:00", "16:00"));

        assertEquals("12:00", response.getHoraInicio());
    }

    // Prueba 7
    @Test
    void actualizarFranja_queQuedaSuperpuestaConOtraFalla() {
        FranjaHoraria tarde = franja(2L, DayOfWeek.MONDAY, "16:00", "20:00");
        when(franjaHorariaRepository.findById(2L)).thenReturn(Optional.of(tarde));
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY))
                .thenReturn(List.of(franja(1L, DayOfWeek.MONDAY, "08:00", "12:00"), tarde));

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.actualizarFranja(2L, request(DayOfWeek.MONDAY, "11:00", "15:00")));

        assertEquals("Esta franja se superpone con 08:00 – 12:00 del lunes.", ex.getMessage());
        assertEquals(h("16:00"), tarde.getHoraInicio()); // no se modificó
        verify(franjaHorariaRepository, never()).save(any());
    }

    // Prueba 10: la franja no choca consigo misma; de 8 a 13 agrega el turno de 12 a 13
    @Test
    void actualizarFranja_deOchoADoceAOchoATreceAgregaUnTurno() {
        FranjaHoraria manana = franja(1L, DayOfWeek.MONDAY, "08:00", "12:00");
        when(franjaHorariaRepository.findById(1L)).thenReturn(Optional.of(manana));
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.MONDAY)).thenReturn(List.of(manana));
        guardarDevuelveLoMismo();

        FranjaHorariaResponse response = franjaHorariaService.actualizarFranja(1L, request(DayOfWeek.MONDAY, "08:00", "13:00"));

        assertEquals(5, response.getCantidadTurnos());
        assertEquals("12:00", response.getTurnos().get(4).getHoraInicio());
        assertEquals("13:00", response.getTurnos().get(4).getHoraFin());
    }

    @Test
    void actualizarFranja_puedeCambiarDeDiaYValidaContraLasDelDiaNuevo() {
        FranjaHoraria lunes = franja(1L, DayOfWeek.MONDAY, "08:00", "12:00");
        when(franjaHorariaRepository.findById(1L)).thenReturn(Optional.of(lunes));
        when(franjaHorariaRepository.findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek.TUESDAY))
                .thenReturn(List.of(franja(5L, DayOfWeek.TUESDAY, "09:00", "10:00")));

        assertThrows(RegistroInvalidoException.class,
                () -> franjaHorariaService.actualizarFranja(1L, request(DayOfWeek.TUESDAY, "08:00", "12:00")));
    }

    @Test
    void actualizarFranja_inexistenteDa404() {
        when(franjaHorariaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> franjaHorariaService.actualizarFranja(99L, request(DayOfWeek.MONDAY, "08:00", "12:00")));
    }

    // Pruebas 11 y 12: eliminar siempre se permite (los turnos generados no dependen de la franja)
    @Test
    void eliminarFranja_borraLaFranja() {
        FranjaHoraria franja = franja(1L, DayOfWeek.MONDAY, "08:00", "12:00");
        when(franjaHorariaRepository.findById(1L)).thenReturn(Optional.of(franja));

        franjaHorariaService.eliminarFranja(1L);

        verify(franjaHorariaRepository).delete(franja);
    }

    // Prueba 9: la vista semanal recibe las franjas ordenadas de lunes a domingo y por hora
    @Test
    void listarFranjas_ordenaPorDiaYHora() {
        when(franjaHorariaRepository.findAll()).thenReturn(List.of(
                franja(1L, DayOfWeek.SATURDAY, "09:00", "12:00"),
                franja(2L, DayOfWeek.MONDAY, "16:00", "20:00"),
                franja(3L, DayOfWeek.MONDAY, "08:00", "12:00")));

        List<FranjaHorariaResponse> franjas = franjaHorariaService.listarFranjas();

        assertEquals(List.of(3L, 2L, 1L), franjas.stream().map(FranjaHorariaResponse::getId).toList());
        assertEquals(3, franjas.get(2).getCantidadTurnos());
    }
}
