package mogni.system_backend.inscripcion.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByMembresiaIdAndEstado(Long membresiaId, EstadoInscripcion estado);

    boolean existsByMembresiaId(Long membresiaId);

    /** Inscripciones en ese estado que empiezan hasta esa fecha (las PROGRAMADA que ya hay que activar). */
    List<Inscripcion> findByEstadoAndFechaInicioLessThanEqual(EstadoInscripcion estado, LocalDate fecha);

    /** Panel del propietario: alumnos de una membresía. */
    interface AlumnosPorMembresia {
        String getMembresia();
        Long getCantidad();
    }

    /**
     * Panel del propietario: alumnos activos por la membresía de su inscripción en ese estado (ACTIVA), de mayor a
     * menor. Las membresías sin alumnos no vienen.
     */
    @Query("""
            select m.nombre as membresia, count(distinct a.id) as cantidad
            from Inscripcion i join i.membresia m join i.alumno a join a.cuenta c
            where i.estado = :estado and c.activa = true
            group by m.id, m.nombre
            order by count(distinct a.id) desc, m.nombre
            """)
    List<AlumnosPorMembresia> contarAlumnosActivosPorMembresia(@Param("estado") EstadoInscripcion estado);
}
