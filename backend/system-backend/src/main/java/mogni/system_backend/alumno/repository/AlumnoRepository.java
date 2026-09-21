package mogni.system_backend.alumno.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.alumno.entity.Alumno;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByMembresiaId(Long membresiaId);

    @Override
    @EntityGraph(attributePaths = {"cuenta", "membresia"})
    List<Alumno> findAll();
}
