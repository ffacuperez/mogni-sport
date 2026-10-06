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
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.service.AlumnoService;
import mogni.system_backend.pago.dto.HistorialPagoResponse;
import mogni.system_backend.pago.service.PagoService;

@RestController
@RequestMapping("/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final PagoService pagoService;

    public AlumnoController(AlumnoService alumnoService, PagoService pagoService) {
        this.alumnoService = alumnoService;
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@Valid @RequestBody RegistrarAlumnoRequest request) {
        AlumnoResponse response = alumnoService.registrarAlumno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    /** Por defecto solo activos; ?activo=false devuelve los dados de baja. */
    public ResponseEntity<List<AlumnoResponse>> listarAlumnos(
            @RequestParam(defaultValue = "true") boolean activo) {
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

    @PutMapping("/{id}/reactivar")
    public ResponseEntity<AlumnoResponse> reactivarAlumno(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.reactivarAlumno(id));
    }

    /** HU-48: historial de pagos del alumno, del más reciente al más antiguo. Solo PROPIETARIO (ver SecurityConfig). */
    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<HistorialPagoResponse>> listarPagos(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.listarHistorialDePagos(id));
    }
}