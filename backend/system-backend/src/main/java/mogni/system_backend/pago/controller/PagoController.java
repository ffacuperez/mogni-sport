package mogni.system_backend.pago.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.pago.dto.PagoResponse;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.service.PagoService;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrarPago(@Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.registrarPago(request));
    }
}
