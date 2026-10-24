package mogni.system_backend.cuota.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    /**
     * Inscripciones de las que sale "hasta cuándo pagó": la activa y la programada (ver 20.10). La membresía que el
     * alumno usa hoy no sale de acá sino de su inscripción ACTIVA.
     */
    List<EstadoInscripcion> ESTADOS_CON_PERIODO_PAGADO = List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PROGRAMADA);

    /**
     * Cuotas con el vencimiento más reciente de cada inscripción en esos estados, de los alumnos con cuenta activa
     * (alumnoId es opcional). Trae inscripción, alumno, cuenta y membresías en una sola consulta. Un alumno puede
     * tener varias (la de la activa y la de la programada, o dos con el mismo vencimiento): usar
     * {@link #buscarUltimasCuotas()}.
     */
    @Query("""
            select c from Cuota c
            join fetch c.inscripcion i
            join fetch i.alumno a
            join fetch a.cuenta cu
            join fetch i.membresia
            where i.estado in :estados
              and cu.activa = true
              and (:alumnoId is null or a.id = :alumnoId)
              and c.fechaVencimiento = (
                  select max(c2.fechaVencimiento) from Cuota c2
                  where c2.inscripcion = i)
            """)
    List<Cuota> buscarConVencimientoMasReciente(@Param("estados") Collection<EstadoInscripcion> estados,
                                                @Param("alumnoId") Long alumnoId);

    /**
     * Hasta cuándo pagó cada alumno activo: su última cuota entre la inscripción activa y la programada, por id de
     * alumno. Con ella se calcula la situación de pago. Su inscripción puede ser la programada: para el nombre de la
     * membresía que usa hoy, ir a la inscripción ACTIVA del alumno.
     */
    default Map<Long, Cuota> buscarUltimasCuotas() {
        return unaPorAlumno(buscarConVencimientoMasReciente(ESTADOS_CON_PERIODO_PAGADO, null));
    }

    /** Lo mismo para un alumno (vacío si está inactivo o no tiene cuotas). */
    default Optional<Cuota> buscarUltimaCuota(Long alumnoId) {
        return Optional.ofNullable(
                unaPorAlumno(buscarConVencimientoMasReciente(ESTADOS_CON_PERIODO_PAGADO, alumnoId)).get(alumnoId));
    }

    private static Map<Long, Cuota> unaPorAlumno(Collection<Cuota> cuotas) {
        BinaryOperator<Cuota> laUltima = (a, b) -> Inscripcion.ORDEN_CUOTAS.compare(a, b) >= 0 ? a : b;
        return cuotas.stream()
                .collect(Collectors.toMap(cuota -> cuota.getInscripcion().getAlumno().getId(), cuota -> cuota, laUltima));
    }
}
