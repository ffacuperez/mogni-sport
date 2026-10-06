package mogni.system_backend.inscripcion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByMembresiaIdAndEstado(Long membresiaId, EstadoInscripcion estado);

    boolean existsByMembresiaId(Long membresiaId);

}
