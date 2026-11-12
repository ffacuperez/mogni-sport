package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas de esos turnos con ese estado (para calcular los lugares libres). */
    List<Reserva> findByTurnoIdInAndEstado(Collection<Long> turnoIds, EstadoReserva estado);

    /** Igual que el anterior, con el alumno y el turno ya cargados (agenda del personal, MOGNI-257). */
    @EntityGraph(attributePaths = {"alumno", "turno"})
    List<Reserva> findWithAlumnoByTurnoIdInAndEstado(Collection<Long> turnoIds, EstadoReserva estado);

    /** Lugares ocupados de un turno (solo cuentan las reservas con ese estado). */
    long countByTurnoIdAndEstado(Long turnoId, EstadoReserva estado);

    /** Reservas del alumno con alguno de esos estados en turnos entre dos fechas (para el contador semanal). */
    @EntityGraph(attributePaths = {"turno"})
    List<Reserva> findByAlumnoIdAndEstadoInAndTurnoFechaBetween(Long alumnoId, Collection<EstadoReserva> estados,
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

    /**
     * HU-262: las reservas con ese estado y sin asistencia registrada de turnos anteriores a la fecha pasan a la
     * asistencia indicada (AUSENTE al terminar el día). Devuelve cuántas cambió. El subselect evita el join
     * implícito, que no se permite en un update.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Reserva r set r.asistencia = :asistencia "
            + "where r.estado = :estado and r.asistencia is null "
            + "and r.turno.id in (select t.id from Turno t where t.fecha < :fecha)")
    int registrarAsistenciaSinMarcarAntesDe(@Param("fecha") LocalDate fecha,
                                            @Param("estado") EstadoReserva estado,
                                            @Param("asistencia") Asistencia asistencia);

    /** Panel del propietario: reservas con una marca de asistencia. */
    interface ReservasPorAsistencia {
        Asistencia getAsistencia();
        Long getCantidad();
    }

    /** Panel del propietario: reservas con ese estado en turnos con ese estado que ya empezaron (mismo rango que el cupo). */
    @Query("""
            select count(r) from Reserva r join r.turno t
            where r.estado = :estadoReserva and t.estado = :estadoTurno and t.fecha >= :desde
              and (t.fecha < :hoy or (t.fecha = :hoy and t.horaInicio <= :hora))
            """)
    long contarEnTurnosComenzados(@Param("estadoReserva") EstadoReserva estadoReserva,
                                  @Param("estadoTurno") EstadoTurno estadoTurno, @Param("desde") LocalDate desde,
                                  @Param("hoy") LocalDate hoy, @Param("hora") LocalTime hora);

    /** Panel del propietario: reservas con ese estado y asistencia marcada en turnos entre dos fechas. */
    @Query("""
            select r.asistencia as asistencia, count(r) as cantidad
            from Reserva r join r.turno t
            where r.estado = :estado and r.asistencia is not null and t.fecha between :desde and :hasta
            group by r.asistencia
            """)
    List<ReservasPorAsistencia> contarPorAsistencia(@Param("estado") EstadoReserva estado,
                                                    @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
