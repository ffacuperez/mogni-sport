package mogni.system_backend.cuota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.service.CuotaService;

@RestController
@RequestMapping("/cuotas")
public class CuotaController {

    private final CuotaService cuotaService;

    public CuotaController(CuotaService cuotaService) {
        this.cuotaService = cuotaService;
    }

    @GetMapping
    public ResponseEntity<List<CuotaResponse>> listarCuotas(
            @RequestParam(required = false) EstadoCuota estado,
            @RequestParam(required = false) Long alumnoId) {
        return ResponseEntity.ok(cuotaService.listarCuotas(estado, alumnoId));
    }
}
