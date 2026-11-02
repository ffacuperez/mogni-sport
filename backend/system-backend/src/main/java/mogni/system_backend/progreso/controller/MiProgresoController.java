package mogni.system_backend.progreso.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.SesionRegistradaResponse;
import mogni.system_backend.progreso.service.MiProgresoService;

/** HU-270: consulta del progreso del alumno logueado. Solo alumnos (ver SecurityConfig). */
@RestController
@RequestMapping("/mi-progreso")
public class MiProgresoController {

    private final MiProgresoService progresoService;

    public MiProgresoController(MiProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    /** El plan activo del alumno (200), o 204 si no tiene ninguno. */
    @GetMapping("/plan-activo")
    public ResponseEntity<PlanDetalleResponse> obtenerPlanActivo(Authentication authentication) {
        return progresoService.obtenerPlanActivo(authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/planes")
    public ResponseEntity<MisPlanesResponse> listarPlanes(Authentication authentication) {
        return ResponseEntity.ok(progresoService.listarPlanes(authentication.getName()));
    }

    /** 404 si el plan no existe o es de otro alumno. */
    @GetMapping("/planes/{id}")
    public ResponseEntity<PlanDetalleResponse> obtenerPlan(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(progresoService.obtenerPlan(authentication.getName(), id));
    }

    /** Las sesiones registradas del plan. 404 si el plan no existe o es de otro alumno. */
    @GetMapping("/planes/{id}/sesiones")
    public ResponseEntity<List<SesionRegistradaResponse>> listarSesiones(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(progresoService.listarSesionesRegistradas(authentication.getName(), id));
    }
}
