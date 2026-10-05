package mogni.system_backend.alumno.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.alumno.entity.Alumno;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);

    @Override
    @EntityGraph(attributePaths = {"cuenta", "inscripciones", "inscripciones.membresia"})
    List<Alumno> findAll();

    @Override
    @EntityGraph(attributePaths = {"cuenta", "inscripciones", "inscripciones.membresia"})
    Optional<Alumno> findById(Long id);

    @EntityGraph(attributePaths = {"cuenta", "inscripciones", "inscripciones.membresia"})
    Optional<Alumno> findByCuentaEmailIgnoreCase(String email);
}
