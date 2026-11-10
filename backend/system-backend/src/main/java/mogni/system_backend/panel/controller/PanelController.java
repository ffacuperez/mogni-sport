package mogni.system_backend.panel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.service.PuestaAlDiaService;
import mogni.system_backend.panel.dto.PanelResponse;
import mogni.system_backend.panel.dto.PlanesPorVencerResponse;
import mogni.system_backend.panel.service.PanelService;
import mogni.system_backend.panel.service.PlanesPorVencerService;

/**
 * Pantalla de Inicio del personal. Los datos del negocio (/panel) son solo del rol PROPIETARIO y los planes por vencer
 * (/panel/planes), del PROPIETARIO y del ENTRENADOR: son dos endpoints para que un entrenador nunca reciba los ingresos
 * (ver SecurityConfig).
 */
@RestController
public class PanelController {

    private final PanelService panelService;
    private final PlanesPorVencerService planesPorVencerService;
    private final PuestaAlDiaService puestaAlDiaService;

    public PanelController(PanelService panelService, PlanesPorVencerService planesPorVencerService,
                           PuestaAlDiaService puestaAlDiaService) {
        this.panelService = panelService;
        this.planesPorVencerService = planesPorVencerService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    @GetMapping("/panel")
    public ResponseEntity<PanelResponse> consultar() {
        // Como en Alumnos y en Mi inicio: programadas activadas y reservas de alumnos en mora canceladas antes de leer
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(panelService.consultar());
    }

    /** Planes de alumnos activos que vencen en los próximos días. Propietario y entrenador: sin datos del negocio. */
    @GetMapping("/panel/planes")
    public ResponseEntity<PlanesPorVencerResponse> planesPorVencer() {
        return ResponseEntity.ok(planesPorVencerService.consultar());
    }
}
