package mogni.system_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.dto.ActualizarAlumnoRequest;
import mogni.system_backend.dto.AlumnoResponse;
import mogni.system_backend.dto.RegistrarAlumnoRequest;
import mogni.system_backend.service.AlumnoService;

@RestController
@RequestMapping("/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> registrarAlumno(@Valid @RequestBody RegistrarAlumnoRequest request) {
        AlumnoResponse response = alumnoService.registrarAlumno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarAlumnos() {
        return ResponseEntity.ok(alumnoService.listarAlumnos());
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
}