package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.turno.entity.ClaseExtra;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ClaseExtraRepository;
import mogni.system_backend.turno.repository.ReservaRepository;

/** Cancelación de reservas por mora o baja: solo turnos que no empezaron, sin clase extra, devolviendo la usada. */
@ExtendWith(MockitoExtension.class)
class CancelacionPorSistemaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private static final LocalDateTime AHORA = HOY.atTime(LocalTime.of(12, 0));

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClaseExtraRepository claseExtraRepository;

    @InjectMocks
    private CancelacionPorSistemaService service;

    private Reserva reserva(Long id, LocalDate fecha, int hora) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setFecha(fecha);
        turno.setHoraInicio(LocalTime.of(hora, 0));
        turno.setHoraFin(LocalTime.of(hora + 1, 0));
        return new Reserva(id, new Alumno(), turno, AHORA.minusDays(1), EstadoReserva.RESERVADA);
    }

    @Test
    void cancela_por_sistema_los_turnos_que_no_empezaron_y_no_toca_los_pasados() {
        Reserva yaEmpezo = reserva(1L, HOY, 11);
        Reserva hoyMasTarde = reserva(2L, HOY, 18);
        Reserva maniana = reserva(3L, HOY.plusDays(1), 8);
        when(reservaRepository.findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(10L, EstadoReserva.RESERVADA, HOY))
                .thenReturn(List.of(yaEmpezo, hoyMasTarde, maniana));

        int canceladas = service.cancelarReservasFuturas(10L, AHORA);

        assertEquals(2, canceladas);
        assertEquals(EstadoReserva.RESERVADA, yaEmpezo.getEstado());
        assertEquals(EstadoReserva.CANCELADA_POR_SISTEMA, hoyMasTarde.getEstado());
        assertEquals(EstadoReserva.CANCELADA_POR_SISTEMA, maniana.getEstado());
        verify(reservaRepository, never()).save(yaEmpezo);
        // No se crea ninguna clase extra
        verify(claseExtraRepository, never()).save(org.mockito.ArgumentMatchers.argThat(c -> c.getId() == null));
    }

    @Test
    void si_la_reserva_uso_una_clase_extra_se_la_devuelve() {
        Reserva conClaseExtra = reserva(2L, HOY.plusDays(1), 8);
        ClaseExtra claseExtra = new ClaseExtra(new Alumno(), 99L, HOY.minusDays(1), HOY.plusDays(6));
        claseExtra.setId(5L);
        claseExtra.setReservaUsoId(2L);
        when(reservaRepository.findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(10L, EstadoReserva.RESERVADA, HOY))
                .thenReturn(List.of(conClaseExtra));
        when(claseExtraRepository.findByReservaUsoId(2L)).thenReturn(Optional.of(claseExtra));

        service.cancelarReservasFuturas(10L, AHORA);

        assertNull(claseExtra.getReservaUsoId());
        verify(claseExtraRepository).save(claseExtra);
    }
}
