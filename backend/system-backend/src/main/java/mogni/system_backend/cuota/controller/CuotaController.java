package mogni.system_backend.cuota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.cuota.service.PuestaAlDiaService;

@RestController
@RequestMapping("/cuotas")
public class CuotaController {

    private final CuotaService cuotaService;
    private final PuestaAlDiaService puestaAlDiaService;

    public CuotaController(CuotaService cuotaService, PuestaAlDiaService puestaAlDiaService) {
        this.cuotaService = cuotaService;
        this.puestaAlDiaService = puestaAlDiaService;
    }

    /** Una fila por alumno activo. Antes se pone al día (bajas automáticas), en su propia transacción. */
    @GetMapping
    public ResponseEntity<List<CuotaResponse>> listarCuotas() {
        puestaAlDiaService.ponerAlDia();
        return ResponseEntity.ok(cuotaService.listarCuotas());
    }
}
