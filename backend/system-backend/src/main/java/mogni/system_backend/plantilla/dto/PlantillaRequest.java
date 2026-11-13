package mogni.system_backend.plantilla.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.plantilla.entity.PlantillaPlan;

@Getter
@Setter
@NoArgsConstructor
public class PlantillaRequest {

    // El @Pattern rechaza nombres que se ven vacíos pero que @NotBlank deja pasar, como los espacios duros
    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El nombre de la plantilla es obligatorio")
    @Size(max = 100, message = "El nombre de la plantilla no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El nivel es obligatorio")
    private NivelPlantilla nivel;

    @NotNull(message = "El objetivo es obligatorio")
    private ObjetivoPlantilla objetivo;

    @NotEmpty(message = "La plantilla tiene que tener al menos una semana")
    @Size(max = PlantillaPlan.MAXIMO_SEMANAS, message = "La plantilla puede tener como máximo 24 semanas")
    private List<@Valid @NotNull(message = "Cada semana tiene que tener al menos una sesión") SemanaRequest> semanas;
}
