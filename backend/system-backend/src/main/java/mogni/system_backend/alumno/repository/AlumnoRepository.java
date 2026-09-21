package mogni.system_backend.alumno.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.alumno.entity.Alumno;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);
}