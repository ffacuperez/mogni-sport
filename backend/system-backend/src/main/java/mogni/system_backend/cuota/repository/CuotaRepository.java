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
     * Cuotas con el vencimiento más reciente de la inscripción en ese estado de cada alumno con cuenta activa
     * (alumnoId es opcional). Trae inscripción, alumno, cuenta y membresías en una sola consulta. Si un alumno tiene
     * dos cuotas con el mismo vencimiento vienen las dos: usar {@link #buscarUltimasCuotas()}.
     */
    @Query("""
            select c from Cuota c
            join fetch c.inscripcion i
            join fetch i.alumno a
            join fetch a.cuenta cu
            join fetch i.membresia
            left join fetch c.membresia
            where i.estado = :estadoInscripcion
              and cu.activa = true
              and (:alumnoId is null or a.id = :alumnoId)
              and c.fechaVencimiento = (
                  select max(c2.fechaVencimiento) from Cuota c2
                  where c2.inscripcion = i)
            """)
    List<Cuota> buscarConVencimientoMasReciente(@Param("estadoInscripcion") EstadoInscripcion estadoInscripcion,
                                                @Param("alumnoId") Long alumnoId);

    /** Última cuota de la inscripción activa de cada alumno activo, por id de alumno. */
    default Map<Long, Cuota> buscarUltimasCuotas() {
        return unaPorAlumno(buscarConVencimientoMasReciente(EstadoInscripcion.ACTIVA, null));
    }

    /** Última cuota de la inscripción activa del alumno (vacío si está inactivo o no tiene cuotas). */
    default Optional<Cuota> buscarUltimaCuota(Long alumnoId) {
        return Optional.ofNullable(
                unaPorAlumno(buscarConVencimientoMasReciente(EstadoInscripcion.ACTIVA, alumnoId)).get(alumnoId));
    }

    private static Map<Long, Cuota> unaPorAlumno(Collection<Cuota> cuotas) {
        BinaryOperator<Cuota> laUltima = (a, b) -> Inscripcion.ORDEN_CUOTAS.compare(a, b) >= 0 ? a : b;
        return cuotas.stream()
                .collect(Collectors.toMap(cuota -> cuota.getInscripcion().getAlumno().getId(), cuota -> cuota, laUltima));
    }
}
