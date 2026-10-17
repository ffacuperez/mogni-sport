package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.turno.entity.Turno;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    /**
     * El turno con la fila bloqueada hasta el fin de la transacción (SELECT ... FOR UPDATE): dos reservas del
     * mismo turno se hacen una detrás de otra, así nunca se supera el cupo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.id = :id")
    Optional<Turno> findByIdConBloqueo(@Param("id") Long id);
}
