package mogni.system_backend.alumno.dto;

import lombok.Getter;
import lombok.Setter;
import mogni.system_backend.pago.entity.MedioDePago;

/**
 * Reactivación (MOGNI-246). Si la última cuota del alumno todavía cubre hoy se reactiva su misma inscripción y estos
 * datos no hacen falta; si no, son obligatorios: se crean una inscripción nueva y su primera cuota pagada.
 */
@Getter
@Setter
public class ReactivarAlumnoRequest {

    private Long membresiaId;

    private MedioDePago medioDePago;
}
