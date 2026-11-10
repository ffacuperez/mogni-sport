package mogni.system_backend.pago.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Pagos del alumno con su cuota y membresías, del más reciente al más antiguo (HU-48). */
    @Query("""
            select p from Pago p
            join fetch p.cuota c
            join fetch c.inscripcion i
            join fetch i.membresia
            where i.alumno.id = :alumnoId
            order by p.fechaPago desc, p.id desc
            """)
    List<Pago> buscarPorAlumno(@Param("alumnoId") Long alumnoId);

    /** El pago de la cuota (cada cuota tiene uno solo; las cuotas viejas que nunca se pagaron, ninguno). */
    Optional<Pago> findFirstByCuotaIdOrderByIdAsc(Long cuotaId);

    /** Panel del propietario: total cobrado en un mes (monto ya incluye el recargo). */
    interface IngresoMensual {
        Integer getAnio();
        Integer getMes();
        BigDecimal getTotal();
    }

    /** Panel del propietario: total y recargos cobrados con un medio de pago. */
    interface IngresoPorMedio {
        MedioDePago getMedio();
        BigDecimal getTotal();
        BigDecimal getRecargos();
    }

    /**
     * Panel del propietario: lo cobrado por mes calendario entre dos fechas. Cuenta todos los pagos (cuotas,
     * renovaciones con recargo y diferencias por cambio de membresía). Los meses sin pagos no vienen.
     */
    @Query("""
            select year(p.fechaPago) as anio, month(p.fechaPago) as mes, sum(p.monto) as total
            from Pago p
            where p.fechaPago between :desde and :hasta
            group by year(p.fechaPago), month(p.fechaPago)
            """)
    List<IngresoMensual> sumarPorMes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Panel del propietario: lo cobrado entre dos fechas, por medio de pago. Un medio sin pagos no viene. */
    @Query("""
            select p.medioDePago as medio, sum(p.monto) as total, sum(p.recargo) as recargos
            from Pago p
            where p.fechaPago between :desde and :hasta
            group by p.medioDePago
            """)
    List<IngresoPorMedio> sumarPorMedioDePago(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
