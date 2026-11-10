package mogni.system_backend.alumno.repository;

import java.time.LocalDate;
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

    /** Panel del propietario: cantidad de alumnos activos (true) o dados de baja (false). */
    long countByCuentaActiva(boolean activa);

    /**
     * Panel del propietario: alumnos nuevos entre dos fechas, es decir, cuya primera inscripción empieza en ese
     * rango. Los cambios de membresía (HU-274), las renovaciones con otra membresía (HU-275) y las reactivaciones
     * (HU-271) crean inscripciones posteriores, así que no cuentan.
     */
    @Query("""
            select count(a) from Alumno a
            where (select min(i.fechaInicio) from Inscripcion i where i.alumno = a) between :desde and :hasta
            """)
    long contarNuevos(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Panel del propietario: alumnos dados de baja entre dos fechas. Sale de cuentas.fecha_baja, que se borra al
     * reactivar: un alumno que se dio de baja y volvió en el mismo mes no cuenta (decidido para el panel).
     */
    @Query("""
            select count(a) from Alumno a
            where a.cuenta.activa = false and a.cuenta.fechaBaja between :desde and :hasta
            """)
    long contarBajas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
