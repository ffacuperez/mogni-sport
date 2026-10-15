package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas de esos turnos con ese estado (para calcular los lugares libres). */
    List<Reserva> findByTurnoIdInAndEstado(Collection<Long> turnoIds, EstadoReserva estado);

    /** Reservas del alumno con ese estado en turnos entre dos fechas (para el contador semanal). */
    @EntityGraph(attributePaths = {"turno"})
    List<Reserva> findByAlumnoIdAndEstadoAndTurnoFechaBetween(Long alumnoId, EstadoReserva estado,
                                                               LocalDate desde, LocalDate hasta);
}
