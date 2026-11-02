package mogni.system_backend.turno.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.turno.dto.AgendaDiaResponse;
import mogni.system_backend.turno.dto.AgendaSemanaResponse;
import mogni.system_backend.turno.dto.AsistenciaResponse;
import mogni.system_backend.turno.dto.CancelacionGimnasioResponse;
import mogni.system_backend.turno.dto.CancelacionGimnasioResumenResponse;
import mogni.system_backend.turno.dto.CancelarDiaRequest;
import mogni.system_backend.turno.dto.CancelarTurnoRequest;
import mogni.system_backend.turno.dto.RegistrarAsistenciaRequest;
import mogni.system_backend.turno.service.AgendaService;
import mogni.system_backend.turno.service.AsistenciaService;
import mogni.system_backend.turno.service.CancelacionGimnasioService;
import mogni.system_backend.turno.service.FranjaHorariaService;

/**
 * MOGNI-257: agenda de turnos. Solo PROPIETARIO y ENTRENADOR (ver SecurityConfig). Las cancelaciones (POST) son
 * solo del propietario; la asistencia (HU-262) la registran los dos.
 */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;
    private final CancelacionGimnasioService cancelacionGimnasioService;
    private final PuestaAlDiaService puestaAlDiaService;
    private final AsistenciaService asistenciaService;

    public AgendaController(AgendaService agendaService, CancelacionGimnasioService cancelacionGimnasioService,
                            PuestaAlDiaService puestaAlDiaService, AsistenciaService asistenciaService) {
        this.agendaService = agendaService;
        this.cancelacionGimnasioService = cancelacionGimnasioService;
        this.puestaAlDiaService = puestaAlDiaService;
        this.asistenciaService = asistenciaService;
    }

    /** ?fecha=YYYY-MM-DD (cualquier fecha). Sin fecha, hoy. */
    @GetMapping("/dia")
    public ResponseEntity<AgendaDiaResponse> consultarDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        // Los turnos cancelados por mora no tienen que aparecer ocupados (rediseño de cuotas, puesta al día)
        puestaAlDiaService.ponerAlDia();
        // HU-262: las reservas de días anteriores que quedaron sin marcar pasan a AUSENTE
        asistenciaService.cerrarDiasAnteriores();
        return ResponseEntity.ok(agendaService.consultarDia(fecha));
    }

    /** ?fecha=YYYY-MM-DD (cualquier día de la semana buscada). Sin fecha, la semana actual. */
    @GetMapping("/semana")
    public ResponseEntity<AgendaSemanaResponse> consultarSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        puestaAlDiaService.ponerAlDia();
        asistenciaService.cerrarDiasAnteriores();
        return ResponseEntity.ok(agendaService.consultarSemana(fecha));
    }

    /**
     * HU-262: marca (o corrige) la asistencia de una reserva de un turno que ya comenzó, hasta el final de su día.
     * Body: { "asistencia": "PRESENTE" | "AUSENTE" }. 409 si el turno todavía no comenzó, ya terminó su día o la
     * reserva está cancelada.
     */
    @PutMapping("/reservas/{reservaId}/asistencia")
    public ResponseEntity<AsistenciaResponse> registrarAsistencia(
            @PathVariable Long reservaId, @Valid @RequestBody RegistrarAsistenciaRequest request) {
        return ResponseEntity.ok(asistenciaService.registrar(reservaId, request.getAsistencia()));
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
