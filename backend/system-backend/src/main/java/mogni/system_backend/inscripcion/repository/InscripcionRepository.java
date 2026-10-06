package mogni.system_backend.inscripcion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByMembresiaIdAndEstado(Long membresiaId, EstadoInscripcion estado);

    boolean existsByMembresiaId(Long membresiaId);

    /** Inscripciones en el estado indicado que todavía no tienen ninguna cuota (creadas antes de HU-45). */
    @Query("select i from Inscripcion i join fetch i.membresia where i.estado = :estado and i.cuotas is empty")
    List<Inscripcion> findSinCuotas(@Param("estado") EstadoInscripcion estado);
}
