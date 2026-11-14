package mogni.system_backend.plantilla.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.entity.SemanaPlantilla;

/** Una semana con sus sesiones; su número sale del orden en que llega en la lista. */
@Getter
@Setter
@NoArgsConstructor
public class SemanaRequest {

    /** Opcional, por ejemplo "Semana de descarga". */
    @Size(max = SemanaPlantilla.NOTA_MAX, message = "La nota de la semana no puede superar los 60 caracteres")
    private String nota;

    @NotEmpty(message = "Cada semana tiene que tener al menos una sesión")
    @Size(max = PlantillaPlan.MAXIMO_SESIONES_POR_SEMANA, message = "Cada semana puede tener como máximo 7 sesiones")
    private List<@Valid @NotNull(message = "Cada sesión tiene que tener al menos un ejercicio") SesionRequest> sesiones;
}
