package mogni.system_backend.cuota.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    /** Bloquea la cuota hasta el fin de la transacción, para que dos pagos simultáneos no la cobren dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuota c where c.id = :id")
    Optional<Cuota> findByIdParaActualizar(@Param("id") Long id);

    /** Cuotas de inscripciones en el estado indicado y alumnos con cuenta activa; estado y alumnoId son opcionales. */
    @Query("""
            select c from Cuota c
            join fetch c.inscripcion i
            join fetch i.alumno a
            join fetch a.cuenta cu
            join fetch i.membresia
            where i.estado = :estadoInscripcion
              and cu.activa = true
              and (:estado is null or c.estado = :estado)
              and (:alumnoId is null or a.id = :alumnoId)
            order by c.fechaVencimiento, a.apellido, a.nombre
            """)
    List<Cuota> buscar(@Param("estadoInscripcion") EstadoInscripcion estadoInscripcion,
                       @Param("estado") EstadoCuota estado,
                       @Param("alumnoId") Long alumnoId);

    /** Pasa de PENDIENTE a ATRASADA todas las cuotas cuyo vencimiento es anterior a hoy. */
    @Transactional
    @Modifying
    @Query("update Cuota c set c.estado = :atrasada where c.estado = :pendiente and c.fechaVencimiento < :hoy")
    int marcarAtrasadas(@Param("hoy") LocalDate hoy,
                        @Param("pendiente") EstadoCuota pendiente,
                        @Param("atrasada") EstadoCuota atrasada);
}
