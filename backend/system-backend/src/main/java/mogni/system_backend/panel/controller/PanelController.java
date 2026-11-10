package mogni.system_backend.panel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.panel.dto.PanelResponse;
import mogni.system_backend.panel.service.PanelService;

/** Panel del propietario: el resumen del mes. Solo rol PROPIETARIO (ver SecurityConfig). */
@RestController
public class PanelController {

    private final PanelService panelService;
    private final PuestaAlDiaService puestaAlDiaService;

    public PanelController(PanelService panelService, PuestaAlDiaService puestaAlDiaService) {
        this.panelService = panelService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    @GetMapping("/panel")
    public ResponseEntity<PanelResponse> consultar() {
        // Como en Alumnos y en Mi inicio: programadas activadas y reservas de alumnos en mora canceladas antes de leer
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(panelService.consultar());
    }
}
