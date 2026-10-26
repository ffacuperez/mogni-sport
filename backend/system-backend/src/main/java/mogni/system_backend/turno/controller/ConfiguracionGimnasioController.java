package mogni.system_backend.turno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.turno.dto.ConfiguracionGimnasioResponse;
import mogni.system_backend.turno.dto.CupoMaximoRequest;
import mogni.system_backend.turno.service.ConfiguracionGimnasioService;

/** HU-252: cupo máximo por turno. Solo PROPIETARIO (ver SecurityConfig). */
@RestController
@RequestMapping("/configuracion-gimnasio")
public class ConfiguracionGimnasioController {

    private final ConfiguracionGimnasioService configuracionGimnasioService;

    public ConfiguracionGimnasioController(ConfiguracionGimnasioService configuracionGimnasioService) {
        this.configuracionGimnasioService = configuracionGimnasioService;
    }

    @GetMapping
    public ResponseEntity<ConfiguracionGimnasioResponse> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionGimnasioService.obtenerConfiguracion());
    }

    /** El nuevo cupo lo toman los turnos futuros sin reservas; los que ya tienen reservas conservan el suyo. */
    @PutMapping("/cupo-maximo")
    public ResponseEntity<ConfiguracionGimnasioResponse> cambiarCupoMaximo(@Valid @RequestBody CupoMaximoRequest request) {
        return ResponseEntity.ok(configuracionGimnasioService.cambiarCupoMaximo(request.getCupoMaximo()));
    }
}
