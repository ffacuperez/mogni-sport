package mogni.system_backend.plantilla.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.plantilla.entity.PlantillaPlan;

public interface PlantillaPlanRepository extends JpaRepository<PlantillaPlan, Long> {

    /** Plantillas no dadas de baja, de la modificada más recientemente a la más antigua. */
    List<PlantillaPlan> findByActivaTrueOrderByFechaActualizacionDesc();

    Optional<PlantillaPlan> findByIdAndActivaTrue(Long id);

    boolean existsByNombreIgnoreCaseAndActivaTrue(String nombre);

    boolean existsByNombreIgnoreCaseAndActivaTrueAndIdNot(String nombre, Long id);

    /** Si el ejercicio se usa en alguna plantilla, incluso dada de baja (sus sesiones siguen guardadas). */
    @Query("select count(es) > 0 from EjercicioSesion es where es.ejercicio.id = :ejercicioId")
    boolean existeEjercicioEnAlgunaPlantilla(@Param("ejercicioId") Long ejercicioId);
}
