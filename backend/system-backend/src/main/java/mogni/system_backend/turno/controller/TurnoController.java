package mogni.system_backend.turno.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.turno.dto.ReservaResponse;
import mogni.system_backend.turno.dto.TurnosDisponiblesResponse;
import mogni.system_backend.turno.service.ConsultaTurnosService;
import mogni.system_backend.turno.service.ReservaService;

/** HU-259: turnos del alumno logueado. Solo rol ALUMNO (ver SecurityConfig); el servicio exige además activo y presencial. */
@RestController
@RequestMapping("/turnos")
public class TurnoController {

    private final ConsultaTurnosService consultaTurnosService;
    private final ReservaService reservaService;

    public TurnoController(ConsultaTurnosService consultaTurnosService, ReservaService reservaService) {
        this.consultaTurnosService = consultaTurnosService;
        this.reservaService = reservaService;
    }

    /** ?fecha=YYYY-MM-DD (hoy o mañana). Sin fecha, hoy. */
    @GetMapping("/disponibles")
    public ResponseEntity<TurnosDisponiblesResponse> consultarTurnosDisponibles(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(consultaTurnosService.consultarTurnosDisponibles(authentication.getName(), fecha));
    }

    /** HU-254: reserva el turno para el alumno logueado. Sin cuerpo: el alumno sale del token. */
    @PostMapping("/{id}/reservar")
    public ResponseEntity<ReservaResponse> reservarTurno(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservar(authentication.getName(), id));
    }
}
