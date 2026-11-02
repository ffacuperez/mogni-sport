package mogni.system_backend.plan.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.plan.dto.AsignarPlanDesdeCeroRequest;
import mogni.system_backend.plan.dto.AsignarPlanDesdePlantillaRequest;
import mogni.system_backend.plan.dto.PlanAsignadoResponse;
import mogni.system_backend.plan.dto.PlanRequest;
import mogni.system_backend.plan.service.PlanAsignadoService;

/** HU-266: planes de entrenamiento asignados a alumnos. Solo propietario y entrenador (ver SecurityConfig). */
@RestController
@RequestMapping("/planes-asignados")
public class PlanAsignadoController {

    private final PlanAsignadoService planService;

    public PlanAsignadoController(PlanAsignadoService planService) {
        this.planService = planService;
    }

    /** El plan activo del alumno (200), o 204 si no tiene ninguno y se le puede asignar uno. */
    @GetMapping("/activo")
    public ResponseEntity<PlanAsignadoResponse> obtenerPlanActivo(@RequestParam Long alumnoId) {
        return planService.obtenerPlanActivo(alumnoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanAsignadoResponse> obtenerPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.obtenerPlan(id));
    }

    /** 409 si el alumno ya tiene un plan activo. */
    @PostMapping("/desde-plantilla")
    public ResponseEntity<PlanAsignadoResponse> asignarDesdePlantilla(
            @Valid @RequestBody AsignarPlanDesdePlantillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.asignarDesdePlantilla(request));
    }

    /** 409 si el alumno ya tiene un plan activo. */
    @PostMapping("/desde-cero")
    public ResponseEntity<PlanAsignadoResponse> asignarDesdeCero(@Valid @RequestBody AsignarPlanDesdeCeroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.asignarDesdeCero(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanAsignadoResponse> actualizarPlan(@PathVariable Long id,
                                                               @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.actualizarPlan(id, request));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<PlanAsignadoResponse> finalizarPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.finalizarPlan(id));
    }
}
