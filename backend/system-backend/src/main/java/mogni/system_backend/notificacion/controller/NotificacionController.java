package mogni.system_backend.notificacion.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.notificacion.dto.DesuscripcionPushRequest;
import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.dto.SuscripcionPushRequest;
import mogni.system_backend.notificacion.service.NotificacionService;
import mogni.system_backend.notificacion.service.PushService;

/** HU-250: notificaciones del alumno logueado. Todo /notificaciones es solo para el rol ALUMNO (ver SecurityConfig). */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final PushService pushService;

    public NotificacionController(NotificacionService notificacionService, PushService pushService) {
        this.notificacionService = notificacionService;
        this.pushService = pushService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(Authentication authentication) {
        return ResponseEntity.ok(notificacionService.listarDelAlumno(authentication.getName()));
    }

    /** Clave pública VAPID para suscribirse a las push. null si el servidor no tiene las push configuradas. */
    @GetMapping("/push/clave-publica")
    public ResponseEntity<Map<String, String>> clavePublicaPush() {
        Map<String, String> body = new HashMap<>(); // Map.of no admite null
        body.put("clavePublica", pushService.clavePublica());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/push/suscripcion")
    public ResponseEntity<Void> suscribirPush(Authentication authentication,
                                              @Valid @RequestBody SuscripcionPushRequest request) {
        pushService.suscribir(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push/suscripcion")
    public ResponseEntity<Void> desuscribirPush(Authentication authentication,
                                                @Valid @RequestBody DesuscripcionPushRequest request) {
        pushService.desuscribir(authentication.getName(), request.getEndpoint());
        return ResponseEntity.noContent().build();
    }

    /** Manda ya mismo una push a los dispositivos del alumno, para probar que llegan. */
    @PostMapping("/push/prueba")
    public ResponseEntity<Map<String, Integer>> enviarPushDePrueba(Authentication authentication) {
        return ResponseEntity.ok(Map.of("enviadas", pushService.enviarPrueba(authentication.getName())));
    }
}
