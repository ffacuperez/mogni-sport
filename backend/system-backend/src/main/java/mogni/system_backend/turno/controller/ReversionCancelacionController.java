package mogni.system_backend.turno.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.turno.dto.ReversionCancelacionResponse;
import mogni.system_backend.turno.service.ReversionCancelacionService;

/**
 * El propietario reactiva un turno que él mismo canceló. Vive bajo /turnos pero es solo PROPIETARIO: la regla va
 * antes que la de /turnos/** (solo alumnos) en SecurityConfig.
 */
@RestController
@RequestMapping("/turnos")
public class ReversionCancelacionController {

    private final ReversionCancelacionService reversionCancelacionService;

    public ReversionCancelacionController(ReversionCancelacionService reversionCancelacionService) {
        this.reversionCancelacionService = reversionCancelacionService;
    }

    @PutMapping("/{id}/revertir-cancelacion")
    public ResponseEntity<ReversionCancelacionResponse> revertirCancelacion(@PathVariable Long id) {
        return ResponseEntity.ok(reversionCancelacionService.revertir(id));
    }
}
