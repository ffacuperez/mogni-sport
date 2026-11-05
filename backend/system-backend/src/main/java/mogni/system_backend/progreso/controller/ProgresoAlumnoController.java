package mogni.system_backend.progreso.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.SesionDetalleResponse;
import mogni.system_backend.progreso.dto.SesionRegistradaResponse;
import mogni.system_backend.progreso.service.MiProgresoService;

/**
 * MOGNI-268: el progreso de un alumno, como lo ve él en Mi progreso (HU-270) pero de solo lectura, para el entrenador
 * y el propietario (ver SecurityConfig). 404 si el alumno no existe, o si el plan o la sesión no son de ese alumno.
 */
@RestController
@RequestMapping("/alumnos/{alumnoId}/progreso")
public class ProgresoAlumnoController {

    private final MiProgresoService progresoService;

    public ProgresoAlumnoController(MiProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    /** El plan activo del alumno (200), o 204 si no tiene ninguno. */
    @GetMapping("/plan-activo")
    public ResponseEntity<PlanDetalleResponse> obtenerPlanActivo(@PathVariable Long alumnoId) {
        return progresoService.obtenerPlanActivoDeAlumno(alumnoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/planes")
    public ResponseEntity<MisPlanesResponse> listarPlanes(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(progresoService.listarPlanesDeAlumno(alumnoId));
    }

    @GetMapping("/planes/{planId}")
    public ResponseEntity<PlanDetalleResponse> obtenerPlan(@PathVariable Long alumnoId, @PathVariable Long planId) {
        return ResponseEntity.ok(progresoService.obtenerPlanDeAlumno(alumnoId, planId));
    }

    /** Las sesiones registradas del plan, de la más reciente a la más antigua. */
    @GetMapping("/planes/{planId}/sesiones")
    public ResponseEntity<List<SesionRegistradaResponse>> listarSesiones(@PathVariable Long alumnoId,
            @PathVariable Long planId) {
        return ResponseEntity.ok(progresoService.listarSesionesRegistradasDeAlumno(alumnoId, planId));
    }

    @GetMapping("/sesiones/{sesionId}")
    public ResponseEntity<SesionDetalleResponse> obtenerSesion(@PathVariable Long alumnoId, @PathVariable Long sesionId) {
        return ResponseEntity.ok(progresoService.obtenerSesionDeAlumno(alumnoId, sesionId));
    }
}
