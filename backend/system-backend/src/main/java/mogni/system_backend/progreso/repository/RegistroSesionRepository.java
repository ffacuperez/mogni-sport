package mogni.system_backend.progreso.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.progreso.entity.RegistroSesion;

public interface RegistroSesionRepository extends JpaRepository<RegistroSesion, Long> {

    List<RegistroSesion> findByPlanId(Long planId);

    /** Una sesión registrada, solo si es de un plan de ese alumno. */
    Optional<RegistroSesion> findByIdAndPlanAlumnoId(Long id, Long alumnoId);

    /** Agrupables por semana: de la semana más reciente a la más antigua y, dentro de cada una, por fecha. */
    List<RegistroSesion> findByPlanIdOrderBySemanaDescFechaDescIdDesc(Long planId);
}
