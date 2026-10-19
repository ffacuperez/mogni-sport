package mogni.system_backend.turno.controller;

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
import mogni.system_backend.turno.dto.FranjaHorariaRequest;
import mogni.system_backend.turno.dto.FranjaHorariaResponse;
import mogni.system_backend.turno.service.FranjaHorariaService;

/** HU-252: franjas horarias semanales. Solo PROPIETARIO (ver SecurityConfig). */
@RestController
@RequestMapping("/franjas")
public class FranjaHorariaController {

    private final FranjaHorariaService franjaHorariaService;

    public FranjaHorariaController(FranjaHorariaService franjaHorariaService) {
        this.franjaHorariaService = franjaHorariaService;
    }

    @GetMapping
    public ResponseEntity<List<FranjaHorariaResponse>> listarFranjas() {
        return ResponseEntity.ok(franjaHorariaService.listarFranjas());
    }

    @PostMapping
    public ResponseEntity<FranjaHorariaResponse> crearFranja(@Valid @RequestBody FranjaHorariaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(franjaHorariaService.crearFranja(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FranjaHorariaResponse> actualizarFranja(
            @PathVariable Long id,
            @Valid @RequestBody FranjaHorariaRequest request) {
        return ResponseEntity.ok(franjaHorariaService.actualizarFranja(id, request));
    }

    /** Siempre se permite: los turnos que quedan sin franja se cancelan al reconciliar (con sus reservas). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFranja(@PathVariable Long id) {
        franjaHorariaService.eliminarFranja(id);
        return ResponseEntity.noContent().build();
    }
}
