package mogni.system_backend.plantilla.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

@Getter
@AllArgsConstructor
public class PlantillaResponse {
    private Long id;
    private String nombre;
    private NivelPlantilla nivel;
    private ObjetivoPlantilla objetivo;
    private int asignacionesActivas;
    private LocalDateTime fechaActualizacion;
    private List<SesionPlantillaResponse> sesiones;
}
