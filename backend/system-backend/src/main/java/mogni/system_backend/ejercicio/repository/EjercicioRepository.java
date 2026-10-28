package mogni.system_backend.ejercicio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mogni.system_backend.ejercicio.entity.Ejercicio;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}