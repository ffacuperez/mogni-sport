package mogni.system_backend.notificacion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.service.NotificacionService;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** HU-250: notificaciones del alumno logueado. Solo para el rol ALUMNO (ver SecurityConfig). */
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(Authentication authentication) {
        return ResponseEntity.ok(notificacionService.listarDelAlumno(authentication.getName()));
    }
}
