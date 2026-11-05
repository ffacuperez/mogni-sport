package mogni.system_backend.progreso.controller;

import java.util.List;

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
import mogni.system_backend.progreso.dto.IniciarSesionRequest;
import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.RegistrarEjercicioRequest;
import mogni.system_backend.progreso.dto.SesionDetalleResponse;
import mogni.system_backend.progreso.dto.SesionRegistradaResponse;
import mogni.system_backend.progreso.dto.SesionSemanaResponse;
import mogni.system_backend.progreso.service.MiProgresoService;

/** HU-270: consulta del progreso del alumno logueado. HU-269: registro de sus sesiones. Solo alumnos (ver SecurityConfig). */
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

    /** 404 si la sesión registrada no existe o es de otro alumno. */
    @GetMapping("/sesiones/{id}")
    public ResponseEntity<SesionDetalleResponse> obtenerSesion(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(progresoService.obtenerSesion(authentication.getName(), id));
    }

    /** HU-269: empieza una sesión de la semana actual (o devuelve la ya empezada). 400 si no se puede registrar. */
    @PostMapping("/planes/{id}/sesiones")
    public ResponseEntity<SesionSemanaResponse> iniciarSesion(@PathVariable Long id,
            @Valid @RequestBody IniciarSesionRequest request, Authentication authentication) {
        return ResponseEntity.ok(progresoService.iniciarSesion(authentication.getName(), id, request));
    }

    /** HU-269: carga o corrige un ejercicio de una sesión en curso. 400 si faltan series o datos. */
    @PutMapping("/sesiones/{id}/ejercicios/{orden}")
    public ResponseEntity<SesionSemanaResponse> registrarEjercicio(@PathVariable Long id, @PathVariable int orden,
            @Valid @RequestBody RegistrarEjercicioRequest request, Authentication authentication) {
        return ResponseEntity.ok(progresoService.registrarEjercicio(authentication.getName(), id, orden, request));
    }

    /** HU-269: finaliza la sesión. 400 si le falta algún ejercicio. */
    @PostMapping("/sesiones/{id}/completar")
    public ResponseEntity<SesionSemanaResponse> completarSesion(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(progresoService.completarSesion(authentication.getName(), id));
    }
}
