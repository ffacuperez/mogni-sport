package mogni.system_backend.membresia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.membresia.entity.Membresia;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
