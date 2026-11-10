package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.entity.Turno;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    /** Fechas desde la indicada que ya tienen turnos generados (las que hay que reconciliar al cambiar la agenda). */
    @Query("select distinct t.fecha from Turno t where t.fecha >= :desde order by t.fecha")
    List<LocalDate> findFechasConTurnosDesde(@Param("desde") LocalDate desde);

    /**
     * El turno con la fila bloqueada hasta el fin de la transacción (SELECT ... FOR UPDATE): dos reservas del
     * mismo turno se hacen una detrás de otra, así nunca se supera el cupo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.id = :id")
    Optional<Turno> findByIdConBloqueo(@Param("id") Long id);

    /**
     * Panel del propietario: suma de los cupos de los turnos en ese estado que ya empezaron, desde una fecha hasta
     * hoy a esa hora. Solo cuenta los turnos generados (se crean a demanda): un día que nadie consultó no tiene.
     */
    @Query("""
            select coalesce(sum(t.cupoMaximo), 0) from Turno t
            where t.estado = :estado and t.fecha >= :desde
              and (t.fecha < :hoy or (t.fecha = :hoy and t.horaInicio <= :hora))
            """)
    long sumarCupoDeTurnosComenzados(@Param("estado") EstadoTurno estado, @Param("desde") LocalDate desde,
                                     @Param("hoy") LocalDate hoy, @Param("hora") LocalTime hora);
}
