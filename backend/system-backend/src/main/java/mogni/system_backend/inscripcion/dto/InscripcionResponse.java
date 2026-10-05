package mogni.system_backend.inscripcion.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;

@Getter
@AllArgsConstructor
public class InscripcionResponse {
    private Long membresiaId;
    private String membresiaNombre;
    private EstadoInscripcion estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
