package mogni.system_backend.turno.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.turno.dto.TurnosDisponiblesResponse;
import mogni.system_backend.turno.service.ConsultaTurnosService;

/** HU-259: turnos del alumno logueado. Solo rol ALUMNO (ver SecurityConfig); el servicio exige además activo y presencial. */
@RestController
@RequestMapping("/turnos")
public class TurnoController {

    private final ConsultaTurnosService consultaTurnosService;

    public TurnoController(ConsultaTurnosService consultaTurnosService) {
        this.consultaTurnosService = consultaTurnosService;
    }

    /** ?fecha=YYYY-MM-DD (hoy o mañana). Sin fecha, hoy. */
    @GetMapping("/disponibles")
    public ResponseEntity<TurnosDisponiblesResponse> consultarTurnosDisponibles(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(consultaTurnosService.consultarTurnosDisponibles(authentication.getName(), fecha));
    }
}
