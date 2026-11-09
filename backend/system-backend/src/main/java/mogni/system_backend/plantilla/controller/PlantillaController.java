package mogni.system_backend.plantilla.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.plantilla.dto.PlantillaRequest;
import mogni.system_backend.plantilla.dto.PlantillaResponse;
import mogni.system_backend.plantilla.service.PlantillaService;

/** HU-265: plantillas de plan de entrenamiento. Solo propietario y entrenador (ver SecurityConfig). */
@RestController
@RequestMapping("/plantillas")
public class PlantillaController {

    private final PlantillaService plantillaService;

    public PlantillaController(PlantillaService plantillaService) {
        this.plantillaService = plantillaService;
    }

    @GetMapping
    public ResponseEntity<List<PlantillaResponse>> listarPlantillas() {
        return ResponseEntity.ok(plantillaService.listarPlantillas());
    }

    /** Plantillas inactivas (para poder reactivarlas). */
    @GetMapping("/inactivas")
    public ResponseEntity<List<PlantillaResponse>> listarInactivas() {
        return ResponseEntity.ok(plantillaService.listarInactivas());
    }

    /** Reactiva una plantilla inactiva; 409 si ya hay una activa con el mismo nombre. */
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<PlantillaResponse> reactivarPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaService.reactivarPlantilla(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaResponse> obtenerPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaService.obtenerPlantilla(id));
    }

    @PostMapping
    public ResponseEntity<PlantillaResponse> crearPlantilla(@Valid @RequestBody PlantillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaService.crearPlantilla(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaResponse> actualizarPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaRequest request) {
        return ResponseEntity.ok(plantillaService.actualizarPlantilla(id, request));
    }

    /** Baja lógica; 409 si la plantilla está asignada a algún alumno. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPlantilla(@PathVariable Long id) {
        plantillaService.eliminarPlantilla(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicar")
    public ResponseEntity<PlantillaResponse> duplicarPlantilla(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaService.duplicarPlantilla(id));
    }
}
