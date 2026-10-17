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

import mogni.system_backend.turno.dto.CancelacionResponse;
import mogni.system_backend.turno.dto.MisTurnosResponse;
import mogni.system_backend.turno.dto.ReservaResponse;
import mogni.system_backend.turno.dto.TurnosDisponiblesResponse;
import mogni.system_backend.turno.service.CancelacionService;
import mogni.system_backend.turno.service.ConsultaTurnosService;
import mogni.system_backend.turno.service.MisTurnosService;
import mogni.system_backend.turno.service.ReservaService;

/** HU-259: turnos del alumno logueado. Solo rol ALUMNO (ver SecurityConfig); el servicio exige además activo y presencial. */
@RestController
@RequestMapping("/turnos")
public class TurnoController {

    private final ConsultaTurnosService consultaTurnosService;
    private final ReservaService reservaService;
    private final MisTurnosService misTurnosService;
    private final CancelacionService cancelacionService;

    public TurnoController(ConsultaTurnosService consultaTurnosService, ReservaService reservaService,
                           MisTurnosService misTurnosService, CancelacionService cancelacionService) {
        this.consultaTurnosService = consultaTurnosService;
        this.reservaService = reservaService;
        this.misTurnosService = misTurnosService;
        this.cancelacionService = cancelacionService;
    }

    /** ?fecha=YYYY-MM-DD (hoy o mañana). Sin fecha, hoy. */
    @GetMapping("/disponibles")
    public ResponseEntity<TurnosDisponiblesResponse> consultarTurnosDisponibles(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(consultaTurnosService.consultarTurnosDisponibles(authentication.getName(), fecha));
    }

    /** HU-256: turnos reservados del alumno logueado que todavía no terminaron, del más cercano al más lejano. */
    @GetMapping("/mis-turnos")
    public ResponseEntity<MisTurnosResponse> consultarMisTurnos(Authentication authentication) {
        return ResponseEntity.ok(misTurnosService.consultarMisTurnos(authentication.getName()));
    }

    /** HU-254: reserva el turno para el alumno logueado. Sin cuerpo: el alumno sale del token. */
    @PostMapping("/{id}/reservar")
    public ResponseEntity<ReservaResponse> reservarTurno(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservar(authentication.getName(), id));
    }

    /** HU-255: el alumno logueado cancela una reserva suya (hasta 2 horas antes del turno). Sin cuerpo. */
    @PostMapping("/reservas/{id}/cancelar")
    public ResponseEntity<CancelacionResponse> cancelarReserva(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(cancelacionService.cancelar(authentication.getName(), id));
    }
}
