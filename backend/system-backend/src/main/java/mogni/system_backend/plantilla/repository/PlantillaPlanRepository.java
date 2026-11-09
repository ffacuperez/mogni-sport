package mogni.system_backend.plantilla.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.plantilla.entity.PlantillaPlan;

public interface PlantillaPlanRepository extends JpaRepository<PlantillaPlan, Long> {

    /** Plantillas activas, de la modificada más recientemente a la más antigua. */
    List<PlantillaPlan> findByActivaTrueOrderByFechaActualizacionDesc();

    /** Plantillas inactivas, de la modificada más recientemente a la más antigua. */
    List<PlantillaPlan> findByActivaFalseOrderByFechaActualizacionDesc();

    Optional<PlantillaPlan> findByIdAndActivaTrue(Long id);

    Optional<PlantillaPlan> findByIdAndActivaFalse(Long id);

    boolean existsByNombreIgnoreCaseAndActivaTrue(String nombre);

    boolean existsByNombreIgnoreCaseAndActivaTrueAndIdNot(String nombre, Long id);

    /** Si el ejercicio se usa en alguna plantilla, incluso inactiva (sus sesiones siguen guardadas). */
    @Query("select count(es) > 0 from EjercicioSesion es where es.ejercicio.id = :ejercicioId")
    boolean existeEjercicioEnAlgunaPlantilla(@Param("ejercicioId") Long ejercicioId);

    // HU-266: asignaciones activas. Se actualizan en la base (y no leyendo y guardando la entidad) para que dos
    // asignaciones simultáneas de la misma plantilla no pisen el contador.

    @Modifying(flushAutomatically = true)
    @Query("update PlantillaPlan p set p.asignacionesActivas = p.asignacionesActivas + 1 where p.id = :id")
    void sumarAsignacion(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update PlantillaPlan p set p.asignacionesActivas = p.asignacionesActivas - 1 "
            + "where p.id = :id and p.asignacionesActivas > 0")
    void restarAsignacion(@Param("id") Long id);
}
