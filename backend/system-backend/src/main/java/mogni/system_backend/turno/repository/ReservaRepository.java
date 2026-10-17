package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas de esos turnos con ese estado (para calcular los lugares libres). */
    List<Reserva> findByTurnoIdInAndEstado(Collection<Long> turnoIds, EstadoReserva estado);

    /** Igual que el anterior, con el alumno y el turno ya cargados (agenda del personal, MOGNI-257). */
    @EntityGraph(attributePaths = {"alumno", "turno"})
    List<Reserva> findWithAlumnoByTurnoIdInAndEstado(Collection<Long> turnoIds, EstadoReserva estado);

    /** Lugares ocupados de un turno (solo cuentan las reservas con ese estado). */
    long countByTurnoIdAndEstado(Long turnoId, EstadoReserva estado);

    /** Reservas del alumno con ese estado en turnos entre dos fechas (para el contador semanal). */
    @EntityGraph(attributePaths = {"turno"})
    List<Reserva> findByAlumnoIdAndEstadoAndTurnoFechaBetween(Long alumnoId, EstadoReserva estado,
                                                               LocalDate desde, LocalDate hasta);

    /** Reservas del alumno con ese estado en turnos de esa fecha en adelante (HU-256, sin ordenar). */
    @EntityGraph(attributePaths = {"turno"})
    List<Reserva> findByAlumnoIdAndEstadoAndTurnoFechaGreaterThanEqual(Long alumnoId, EstadoReserva estado,
                                                                        LocalDate desde);

    /**
     * La reserva con la fila bloqueada hasta el fin de la transacción (SELECT ... FOR UPDATE): dos cancelaciones de
     * la misma reserva (doble clic) se hacen una detrás de otra y la segunda la encuentra ya cancelada.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdConBloqueo(@Param("id") Long id);
}
