package mogni.system_backend.membresia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.membresia.dto.MembresiaRequest;
import mogni.system_backend.membresia.dto.MembresiaResponse;
import mogni.system_backend.membresia.service.MembresiaService;

@RestController
@RequestMapping("/membresias")
public class MembresiaController {

    private final MembresiaService membresiaService;

    public MembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    @PostMapping
    public ResponseEntity<MembresiaResponse> crearMembresia(@Valid @RequestBody MembresiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membresiaService.crearMembresia(request));
    }

    @GetMapping
    public ResponseEntity<List<MembresiaResponse>> listarMembresias() {
        return ResponseEntity.ok(membresiaService.listarMembresias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembresiaResponse> obtenerMembresia(@PathVariable Long id) {
        return ResponseEntity.ok(membresiaService.obtenerMembresia(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembresiaResponse> actualizarMembresia(
            @PathVariable Long id,
            @Valid @RequestBody MembresiaRequest request) {
        return ResponseEntity.ok(membresiaService.actualizarMembresia(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMembresia(@PathVariable Long id) {
        membresiaService.eliminarMembresia(id);
        return ResponseEntity.noContent().build();
    }
}
