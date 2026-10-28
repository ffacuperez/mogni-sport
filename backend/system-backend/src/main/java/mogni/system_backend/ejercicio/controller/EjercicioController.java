package mogni.system_backend.ejercicio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.ejercicio.dto.EjercicioRequest;
import mogni.system_backend.ejercicio.dto.EjercicioResponse;
import mogni.system_backend.ejercicio.service.EjercicioService;

@RestController
@RequestMapping("/ejercicios")
public class EjercicioController {

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @PostMapping
    public ResponseEntity<EjercicioResponse> crearEjercicio(@Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ejercicioService.crearEjercicio(request));
    }

    @GetMapping
    public ResponseEntity<List<EjercicioResponse>> listarEjercicios() {
        return ResponseEntity.ok(ejercicioService.listarEjercicios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EjercicioResponse> obtenerEjercicio(@PathVariable Long id) {
        return ResponseEntity.ok(ejercicioService.obtenerEjercicio(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EjercicioResponse> actualizarEjercicio(
            @PathVariable Long id,
            @Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.ok(ejercicioService.actualizarEjercicio(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EjercicioResponse> toggleEstadoEjercicio(@PathVariable Long id) {
        return ResponseEntity.ok(ejercicioService.toggleEstadoEjercicio(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEjercicio(@PathVariable Long id) {
        ejercicioService.eliminarEjercicio(id);
        return ResponseEntity.noContent().build();
    }
}