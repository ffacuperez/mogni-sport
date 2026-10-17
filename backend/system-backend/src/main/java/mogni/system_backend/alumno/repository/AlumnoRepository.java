package mogni.system_backend.alumno.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Alumnos activos (true) o dados de baja (false), según el estado de su cuenta. */
    @EntityGraph(attributePaths = {"cuenta", "inscripciones", "inscripciones.membresia"})
    List<Alumno> findByCuentaActiva(boolean activa);

    /**
     * Bloquea la fila del alumno hasta el fin de la transacción: dos reservas del mismo alumno a la vez
     * (doble clic, dos dispositivos) se hacen una detrás de otra y no se saltean las reglas por día y por semana.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alumno a where a.id = :id")
    Optional<Alumno> findByIdConBloqueo(@Param("id") Long id);
}
