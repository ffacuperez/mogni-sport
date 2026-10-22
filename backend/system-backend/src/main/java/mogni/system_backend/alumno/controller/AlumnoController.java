package mogni.system_backend.alumno.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;
import mogni.system_backend.alumno.dto.AlumnoResponse;
import mogni.system_backend.alumno.dto.CambiarMembresiaRequest;
import mogni.system_backend.alumno.dto.CambioMembresiaResponse;
import mogni.system_backend.alumno.dto.ReactivarAlumnoRequest;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.service.AlumnoService;
import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.pago.dto.HistorialPagoResponse;
import mogni.system_backend.pago.dto.RenovacionResponse;
import mogni.system_backend.pago.service.PagoService;

@RestController
@RequestMapping("/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final PagoService pagoService;
    private final PuestaAlDiaService puestaAlDiaService;

    public AlumnoController(AlumnoService alumnoService, PagoService pagoService,
                            PuestaAlDiaService puestaAlDiaService) {
        this.alumnoService = alumnoService;
        this.pagoService = pagoService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@Valid @RequestBody RegistrarAlumnoRequest request) {
        AlumnoResponse response = alumnoService.registrarAlumno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Por defecto solo activos; ?activo=false devuelve los dados de baja. Antes se pone al día (bajas automáticas por
     * mora), en su propia transacción.
     */
    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarAlumnos(
            @RequestParam(defaultValue = "true") boolean activo) {
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(alumnoService.listarAlumnos(activo));
    }

    /** Datos del alumno logueado. Solo para el rol ALUMNO (ver SecurityConfig). */
    @GetMapping("/me")
    public ResponseEntity<AlumnoResponse> obtenerMiPerfil(Authentication authentication) {
        return ResponseEntity.ok(alumnoService.obtenerAlumnoPorEmail(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtenerAlumno(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.obtenerAlumno(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizarAlumno(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.actualizarAlumno(id, request));
    }

    /** Baja lógica: el alumno queda inactivo, no se elimina de la base. */
    @DeleteMapping("/{id}")
    public ResponseEntity<AlumnoResponse> darDeBajaAlumno(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.darDeBajaAlumno(id));
    }

    /** El cuerpo (membresía y medio de pago) solo es obligatorio si el alumno ya no tiene días pagos. */
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<AlumnoResponse> reactivarAlumno(@PathVariable Long id,
            @RequestBody(required = false) ReactivarAlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.reactivarAlumno(id, request));
    }

    /**
     * Cambio de membresía (reglas M1 a M9). Endpoint propio: no toca ni valida los datos personales del alumno, que se
     * modifican con PUT /alumnos/{id}.
     */
    @PutMapping("/{id}/membresia")
    public ResponseEntity<AlumnoResponse> cambiarMembresia(@PathVariable Long id,
            @Valid @RequestBody CambiarMembresiaRequest request) {
        return ResponseEntity.ok(alumnoService.cambiarMembresia(id, request));
    }

    /** Vista previa del cambio de membresía (aviso de confirmación antes de guardar). */
    @GetMapping("/{id}/cambio-membresia")
    public ResponseEntity<CambioMembresiaResponse> previsualizarCambioMembresia(
            @PathVariable Long id, @RequestParam Long membresiaId) {
        return ResponseEntity.ok(alumnoService.previsualizarCambioMembresia(id, membresiaId));
    }

    /** MOGNI-45: vista previa de la renovación (período, precio, recargo y total). Solo PROPIETARIO (ver SecurityConfig). */
    @GetMapping("/{id}/renovacion")
    public ResponseEntity<RenovacionResponse> previsualizarRenovacion(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.previsualizarRenovacion(id));
    }

    /** HU-48: historial de pagos del alumno, del más reciente al más antiguo. Solo PROPIETARIO (ver SecurityConfig). */
    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<HistorialPagoResponse>> listarPagos(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.listarHistorialDePagos(id));
    }
}