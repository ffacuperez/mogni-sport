package mogni.system_backend.turno.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.turno.dto.AgendaDiaResponse;
import mogni.system_backend.turno.dto.AgendaSemanaResponse;
import mogni.system_backend.turno.dto.CancelacionGimnasioResponse;
import mogni.system_backend.turno.dto.CancelacionGimnasioResumenResponse;
import mogni.system_backend.turno.dto.CancelarDiaRequest;
import mogni.system_backend.turno.dto.CancelarTurnoRequest;
import mogni.system_backend.turno.service.AgendaService;
import mogni.system_backend.turno.service.CancelacionGimnasioService;
import mogni.system_backend.turno.service.FranjaHorariaService;

/** MOGNI-257: agenda de turnos. Solo PROPIETARIO y ENTRENADOR (ver SecurityConfig). */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;
    private final CancelacionGimnasioService cancelacionGimnasioService;
    private final PuestaAlDiaService puestaAlDiaService;

    public AgendaController(AgendaService agendaService, CancelacionGimnasioService cancelacionGimnasioService,
                            PuestaAlDiaService puestaAlDiaService) {
        this.agendaService = agendaService;
        this.cancelacionGimnasioService = cancelacionGimnasioService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    /** ?fecha=YYYY-MM-DD (cualquier fecha). Sin fecha, hoy. */
    @GetMapping("/dia")
    public ResponseEntity<AgendaDiaResponse> consultarDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        // Los turnos cancelados por mora no tienen que aparecer ocupados (rediseño de cuotas, puesta al día)
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(agendaService.consultarDia(fecha));
    }

    /** ?fecha=YYYY-MM-DD (cualquier día de la semana buscada). Sin fecha, la semana actual. */
    @GetMapping("/semana")
    public ResponseEntity<AgendaSemanaResponse> consultarSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(agendaService.consultarSemana(fecha));
    }

    @GetMapping("/cancelacion/resumen")
    public ResponseEntity<CancelacionGimnasioResumenResponse> resumenCancelarTurno(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio) {
        return ResponseEntity.ok(cancelacionGimnasioService.resumenTurno(fecha, FranjaHorariaService.formatearHora(horaInicio)));
    }

    @GetMapping("/cancelacion/dia/resumen")
    public ResponseEntity<CancelacionGimnasioResumenResponse> resumenCancelarDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(cancelacionGimnasioService.resumenDia(fecha));
    }

    @PostMapping("/cancelar-turno")
    public ResponseEntity<CancelacionGimnasioResponse> cancelarTurno(
            @Valid @RequestBody CancelarTurnoRequest request) {
        return ResponseEntity.ok(
                cancelacionGimnasioService.cancelarTurno(request.getFecha(), request.getHoraInicio()));
    }

    @PostMapping("/cancelar-dia")
    public ResponseEntity<CancelacionGimnasioResponse> cancelarDia(
            @Valid @RequestBody CancelarDiaRequest request) {
        return ResponseEntity.ok(cancelacionGimnasioService.cancelarDia(request.getFecha()));
    }
}
