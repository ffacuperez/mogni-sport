package mogni.system_backend.inscripcion.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByMembresiaIdAndEstado(Long membresiaId, EstadoInscripcion estado);

    boolean existsByMembresiaId(Long membresiaId);

    /** Inscripciones en ese estado que empiezan hasta esa fecha (las PROGRAMADA que ya hay que activar). */
    List<Inscripcion> findByEstadoAndFechaInicioLessThanEqual(EstadoInscripcion estado, LocalDate fecha);
}
