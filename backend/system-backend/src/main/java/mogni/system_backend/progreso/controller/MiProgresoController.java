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
import mogni.system_backend.progreso.dto.MisPlanesResponse;
import mogni.system_backend.progreso.dto.PlanDetalleResponse;
import mogni.system_backend.progreso.dto.RegistroEjercicioRequest;
import mogni.system_backend.progreso.dto.RegistroSesionRequest;
import mogni.system_backend.progreso.dto.RegistroSesionResponse;
import mogni.system_backend.progreso.dto.SesionDetalleResponse;
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

    /** 404 si la sesión registrada no existe o es de otro alumno. */
    @GetMapping("/sesiones/{id}")
    public ResponseEntity<SesionDetalleResponse> obtenerSesion(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(progresoService.obtenerSesion(authentication.getName(), id));
    }

    /** Inicio de una sesión del plan activo para completar el ejercicio. */
    @PostMapping("/planes/{planId}/sesiones")
    public ResponseEntity<RegistroSesionResponse> iniciarSesion(@PathVariable Long planId,
            @Valid @RequestBody RegistroSesionRequest request, Authentication authentication) {
        return ResponseEntity.ok(progresoService.iniciarSesion(authentication.getName(), planId, request));
    }

    /** Guarda o actualiza el registro del ejercicio dentro de una sesión. */
    @PutMapping("/sesiones/{registroId}/ejercicios/{orden}")
    public ResponseEntity<RegistroSesionResponse> guardarEjercicio(@PathVariable Long registroId,
            @PathVariable int orden, @Valid @RequestBody RegistroEjercicioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(progresoService.guardarEjercicio(authentication.getName(), registroId, orden, request));
    }

    /** Cierra la sesión solo si todos los ejercicios están completados. */
    @PostMapping("/sesiones/{registroId}/completar")
    public ResponseEntity<RegistroSesionResponse> completarSesion(@PathVariable Long registroId,
            Authentication authentication) {
        return ResponseEntity.ok(progresoService.completarSesion(authentication.getName(), registroId));
    }
}
