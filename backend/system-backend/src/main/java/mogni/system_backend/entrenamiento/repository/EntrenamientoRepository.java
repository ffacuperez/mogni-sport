package mogni.system_backend.entrenamiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.entrenamiento.entity.Entrenamiento;
import mogni.system_backend.entrenamiento.entity.EstadoEntrenamiento;

public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {

    @Query("select e from Entrenamiento e where e.alumno.id = :alumnoId and e.sesion.id = :sesionId and e.estado = :estado order by e.fechaCreacion desc")
    Optional<Entrenamiento> findFirstByAlumnoAndSesionAndEstado(@Param("alumnoId") Long alumnoId,
            @Param("sesionId") Long sesionId,
            @Param("estado") EstadoEntrenamiento estado);

    @Query("select e from Entrenamiento e where e.alumno.id = :alumnoId and e.sesion.id = :sesionId order by e.fechaCreacion desc")
    List<Entrenamiento> findByAlumnoAndSesion(@Param("alumnoId") Long alumnoId, @Param("sesionId") Long sesionId);

    @Query("select e from Entrenamiento e where e.alumno.id = :alumnoId and e.plan.id = :planId order by e.fechaCreacion desc")
    List<Entrenamiento> findByAlumnoAndPlan(@Param("alumnoId") Long alumnoId, @Param("planId") Long planId);

    @Query("select e from Entrenamiento e left join fetch e.ejercicios ej left join fetch ej.series s where e.alumno.id = :alumnoId and e.plan.id = :planId and e.estado = :estado order by e.fechaCreacion desc")
    List<Entrenamiento> findByAlumnoAndPlanAndEstadoFetchData(@Param("alumnoId") Long alumnoId,
            @Param("planId") Long planId,
            @Param("estado") EstadoEntrenamiento estado);
}
