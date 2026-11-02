package mogni.system_backend.plan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;

public interface PlanAsignadoRepository extends JpaRepository<PlanAsignado, Long> {

    /** El plan ACTIVO del alumno, si tiene (como mucho hay uno). */
    Optional<PlanAsignado> findFirstByAlumnoIdAndEstado(Long alumnoId, EstadoPlanAsignado estado);

    /** Todos los planes en ese estado; el listado de alumnos los usa para no consultar alumno por alumno. */
    List<PlanAsignado> findByEstado(EstadoPlanAsignado estado);

    /** Si el ejercicio se usa en el plan de algún alumno, activo o finalizado (sus sesiones siguen guardadas). */
    @Query("select count(e) > 0 from EjercicioSesionPlan e where e.ejercicio.id = :ejercicioId")
    boolean existeEjercicioEnAlgunPlan(@Param("ejercicioId") Long ejercicioId);
}
