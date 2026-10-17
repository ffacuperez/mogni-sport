package mogni.system_backend.turno.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.turno.dto.AgendaDiaResponse;
import mogni.system_backend.turno.dto.AgendaSemanaResponse;
import mogni.system_backend.turno.service.AgendaService;

/** MOGNI-257: agenda de turnos. Solo PROPIETARIO y ENTRENADOR (ver SecurityConfig). */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    /** ?fecha=YYYY-MM-DD (cualquier fecha). Sin fecha, hoy. */
    @GetMapping("/dia")
    public ResponseEntity<AgendaDiaResponse> consultarDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendaService.consultarDia(fecha));
    }

    /** ?fecha=YYYY-MM-DD (cualquier día de la semana buscada). Sin fecha, la semana actual. */
    @GetMapping("/semana")
    public ResponseEntity<AgendaSemanaResponse> consultarSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendaService.consultarSemana(fecha));
    }
}
