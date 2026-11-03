package mogni.system_backend.entrenamiento.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.entrenamiento.dto.EntrenamientoRequest;
import mogni.system_backend.entrenamiento.dto.MiPlanEntrenamientoResponse;
import mogni.system_backend.entrenamiento.service.EntrenamientoService;

@RestController
@RequestMapping("/entrenamientos")
public class EntrenamientoController {

    private final EntrenamientoService entrenamientoService;

    public EntrenamientoController(EntrenamientoService entrenamientoService) {
        this.entrenamientoService = entrenamientoService;
    }

    @GetMapping("/mi-plan")
    public ResponseEntity<MiPlanEntrenamientoResponse> obtenerMiPlan(Authentication authentication) {
        Long alumnoId = entrenamientoService.obtenerAlumnoIdPorEmail(authentication.getName());
        return ResponseEntity.ok(entrenamientoService.obtenerMiPlan(alumnoId));
    }

    @PostMapping("/sesiones/{sesionId}/iniciar")
    public ResponseEntity<MiPlanEntrenamientoResponse> iniciarEntrenamiento(@PathVariable Long sesionId,
            Authentication authentication) {
        Long alumnoId = entrenamientoService.obtenerAlumnoIdPorEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(entrenamientoService.iniciarEntrenamiento(alumnoId, sesionId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MiPlanEntrenamientoResponse> guardarEntrenamiento(@PathVariable Long id,
            @Valid @RequestBody EntrenamientoRequest request,
            Authentication authentication) {
        Long alumnoId = entrenamientoService.obtenerAlumnoIdPorEmail(authentication.getName());
        return ResponseEntity.ok(entrenamientoService.guardarEntrenamiento(alumnoId, id, request));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<MiPlanEntrenamientoResponse> finalizarEntrenamiento(@PathVariable Long id,
            Authentication authentication) {
        Long alumnoId = entrenamientoService.obtenerAlumnoIdPorEmail(authentication.getName());
        return ResponseEntity.ok(entrenamientoService.finalizarEntrenamiento(alumnoId, id));
    }
}
