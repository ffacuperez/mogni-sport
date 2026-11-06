package mogni.system_backend.inicio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.inicio.dto.MiInicioResponse;
import mogni.system_backend.inicio.service.MiInicioService;

/**
 * MOGNI-281: el inicio del alumno logueado. Solo rol ALUMNO (ver SecurityConfig); el alumno sale del token, nunca de un
 * parámetro, así que cada uno ve solo lo suyo. El servicio exige además cuenta activa.
 */
@RestController
public class MiInicioController {

    private final MiInicioService miInicioService;
    private final PuestaAlDiaService puestaAlDiaService;

    public MiInicioController(MiInicioService miInicioService, PuestaAlDiaService puestaAlDiaService) {
        this.miInicioService = miInicioService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    @GetMapping("/mi-inicio")
    public ResponseEntity<MiInicioResponse> consultar(Authentication authentication) {
        // Como en los turnos: membresías programadas activadas y reservas de alumnos en mora canceladas antes de leer
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(miInicioService.consultar(authentication.getName()));
    }
}
