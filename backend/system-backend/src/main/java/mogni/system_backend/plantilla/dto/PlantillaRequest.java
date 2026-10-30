package mogni.system_backend.plantilla.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

@Getter
@Setter
@NoArgsConstructor
public class PlantillaRequest {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 100, message = "El nombre de la plantilla no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El nivel es obligatorio")
    private NivelPlantilla nivel;

    @NotNull(message = "El objetivo es obligatorio")
    private ObjetivoPlantilla objetivo;

    @NotEmpty(message = "La plantilla tiene que tener al menos una sesión")
    private List<@Valid SesionRequest> sesiones;
}
