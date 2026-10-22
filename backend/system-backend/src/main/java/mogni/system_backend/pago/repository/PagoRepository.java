package mogni.system_backend.pago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Pagos del alumno con su cuota y membresías, del más reciente al más antiguo (HU-48). */
    @Query("""
            select p from Pago p
            join fetch p.cuota c
            join fetch c.inscripcion i
            join fetch i.membresia
            left join fetch c.membresia
            where i.alumno.id = :alumnoId
            order by p.fechaPago desc, p.id desc
            """)
    List<Pago> buscarPorAlumno(@Param("alumnoId") Long alumnoId);

    /** El pago de la cuota (cada cuota tiene uno solo; las cuotas viejas que nunca se pagaron, ninguno). */
    Optional<Pago> findFirstByCuotaIdOrderByIdAsc(Long cuotaId);
}
