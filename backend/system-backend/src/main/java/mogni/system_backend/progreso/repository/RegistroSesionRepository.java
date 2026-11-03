package mogni.system_backend.progreso.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.progreso.entity.RegistroSesion;

public interface RegistroSesionRepository extends JpaRepository<RegistroSesion, Long> {

    /** Todas, también las que están en curso. */
    List<RegistroSesion> findByPlanId(Long planId);

    /** HU-269: el registro de una sesión en una semana; hay como mucho uno (restricción única). */
    Optional<RegistroSesion> findByPlanIdAndSemanaAndNumeroSesion(Long planId, int semana, int numeroSesion);

    /** Una sesión registrada o en curso, solo si es de un plan de ese alumno. */
    Optional<RegistroSesion> findByIdAndPlanAlumnoId(Long id, Long alumnoId);

    /**
     * Solo las finalizadas, agrupables por semana: de la semana más reciente a la más antigua y, dentro de cada una,
     * por fecha.
     */
    List<RegistroSesion> findByPlanIdAndCompletadaTrueOrderBySemanaDescFechaDescIdDesc(Long planId);
}
