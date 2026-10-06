package mogni.system_backend.pago.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Pagos del alumno, del más reciente al más antiguo (HU-48). */
    @Query("""
            select p from Pago p
            join fetch p.cuota c
            where c.inscripcion.alumno.id = :alumnoId
            order by p.fechaPago desc, p.id desc
            """)
    List<Pago> buscarPorAlumno(@Param("alumnoId") Long alumnoId);
}
