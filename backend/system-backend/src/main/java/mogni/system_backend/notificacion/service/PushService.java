package mogni.system_backend.notificacion.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.notificacion.dto.NotificacionResponse;
import mogni.system_backend.notificacion.dto.SuscripcionPushRequest;
import mogni.system_backend.notificacion.entity.AvisoPushEnviado;
import mogni.system_backend.notificacion.entity.SuscripcionPush;
import mogni.system_backend.notificacion.repository.AvisoPushEnviadoRepository;
import mogni.system_backend.notificacion.repository.SuscripcionPushRepository;

/**
 * Notificaciones push al celular (HU-250). Son el mismo aviso que el alumno ve en la app, pero le llega aunque
 * la tenga cerrada. Se manda una sola vez por cuota y estado: al pasar a "por vencer" y al pasar a "vencida".
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final String URL_NOTIFICACIONES = "/mi-cuenta/notificaciones";

    private final WebPushSender sender;
    private final SuscripcionPushRepository suscripcionRepository;
    private final AvisoPushEnviadoRepository avisoRepository;
    private final CuentaRepository cuentaRepository;
    private final CuotaRepository cuotaRepository;
    private final NotificacionService notificacionService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public PushService(WebPushSender sender, SuscripcionPushRepository suscripcionRepository,
                       AvisoPushEnviadoRepository avisoRepository, CuentaRepository cuentaRepository,
                       CuotaRepository cuotaRepository, NotificacionService notificacionService) {
        this.sender = sender;
        this.suscripcionRepository = suscripcionRepository;
        this.avisoRepository = avisoRepository;
        this.cuentaRepository = cuentaRepository;
        this.cuotaRepository = cuotaRepository;
        this.notificacionService = notificacionService;
    }

    /** Clave pública VAPID para que el navegador se suscriba; null si las push no están configuradas. */
    public String clavePublica() {
        return sender.estaConfigurado() ? sender.getClavePublica() : null;
    }

    /**
     * Guarda el dispositivo del alumno. Si ese dispositivo ya estaba registrado (p. ej. otra cuenta inició sesión
     * antes en el mismo celular), pasa a ser de esta cuenta: las push le llegan a quien usa el celular ahora.
     */
    @Transactional
    public void suscribir(String email, SuscripcionPushRequest request) {
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta"));
        SuscripcionPush suscripcion = suscripcionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(SuscripcionPush::new);
        suscripcion.setCuenta(cuenta);
        suscripcion.setEndpoint(request.getEndpoint());
        suscripcion.setP256dh(request.getP256dh());
        suscripcion.setAuth(request.getAuth());
        suscripcion.setFechaAlta(LocalDateTime.now(ZoneId.of(ZonaHoraria.ZONA)));
        suscripcionRepository.save(suscripcion);
    }

    public void desuscribir(String email, String endpoint) {
        suscripcionRepository.deleteByEndpointAndCuentaEmailIgnoreCase(endpoint, email);
    }

    /**
     * Manda ya mismo a los dispositivos del alumno sus notificaciones vigentes (o un mensaje de prueba si no
     * tiene ninguna). Sirve para probar que las push funcionan; no cuenta como aviso enviado.
     */
    public int enviarPrueba(String email) {
        List<SuscripcionPush> suscripciones = suscripcionRepository.findByCuentaEmailIgnoreCase(email);
        if (suscripciones.isEmpty()) {
            throw new RegistroInvalidoException("No hay ningún dispositivo con las notificaciones activadas");
        }
        List<NotificacionResponse> vigentes = notificacionService.listarDelAlumno(email);
        List<String> mensajes = vigentes.isEmpty()
                ? List.of(mensaje("¡Las notificaciones funcionan!", "Te vamos a avisar acá cuando tu cuota esté por vencer.", "prueba"))
                : vigentes.stream().map(this::mensajeDe).toList();

        int enviadas = 0;
        for (SuscripcionPush suscripcion : suscripciones) {
            for (String mensaje : mensajes) {
                if (enviar(suscripcion, mensaje)) enviadas++;
            }
        }
        return enviadas;
    }

    /** Manda un aviso a todos los dispositivos de la cuenta; devuelve a cuántos llegó. Sin push configuradas, a ninguno. */
    public int enviarAviso(Long cuentaId, String tag, String titulo, String texto) {
        if (!sender.estaConfigurado()) {
            return 0;
        }
        String mensaje = mensaje(titulo, texto, tag);
        int enviadas = 0;
        for (SuscripcionPush suscripcion : suscripcionRepository.findByCuentaId(cuentaId)) {
            if (enviar(suscripcion, mensaje)) enviadas++;
        }
        return enviadas;
    }

    /**
     * Todos los días a las 10:00 (hora de Argentina). La situación de pago se calcula con la fecha (no hay tarea que
     * marque estados), así que no depende del orden respecto de la tarea de la 01:00.
     * Por cada última cuota con notificación vigente que todavía no se avisó con ese tipo, manda la push.
     * Si el alumno no tiene dispositivos registrados no se marca como enviada: le llega cuando active las push.
     */
    @Scheduled(cron = "0 0 10 * * *", zone = ZonaHoraria.ZONA)
    public void enviarAvisosDeCuotas() {
        if (!sender.estaConfigurado()) {
            return;
        }
        LocalDate hoy = ZonaHoraria.hoy();
        int enviados = 0;
        // Solo la última cuota de cada alumno: las de meses ya pagados no tienen que avisar "vencida"
        for (Cuota cuota : cuotaRepository.buscarUltimasCuotas().values()) {
            NotificacionResponse notificacion = notificacionService.notificacionDe(cuota, hoy).orElse(null);
            if (notificacion == null || avisoRepository.existsByCuotaIdAndTipo(cuota.getId(), notificacion.getTipo())) {
                continue;
            }
            Long cuentaId = cuota.getInscripcion().getAlumno().getCuenta().getId();
            boolean alguno = false;
            for (SuscripcionPush suscripcion : suscripcionRepository.findByCuentaId(cuentaId)) {
                alguno |= enviar(suscripcion, mensajeDe(notificacion));
            }
            if (alguno) {
                avisoRepository.save(new AvisoPushEnviado(cuota.getId(), notificacion.getTipo(),
                        LocalDateTime.now(ZoneId.of(ZonaHoraria.ZONA))));
                enviados++;
            }
        }
        if (enviados > 0) {
            log.info("Se enviaron {} avisos push de cuotas", enviados);
        }
    }

    /** Devuelve si llegó. Si el dispositivo ya no existe, se borra la suscripción; cualquier otro error solo se loguea. */
    private boolean enviar(SuscripcionPush suscripcion, String mensaje) {
        try {
            WebPushSender.Resultado resultado = sender.enviar(
                    suscripcion.getEndpoint(), suscripcion.getP256dh(), suscripcion.getAuth(), mensaje);
            if (resultado.suscripcionVencida()) {
                log.info("Suscripción push {} vencida (HTTP {}), se elimina", suscripcion.getId(), resultado.status());
                suscripcionRepository.deleteByEndpoint(suscripcion.getEndpoint());
            } else if (!resultado.enviado()) {
                log.warn("El servicio de push rechazó el envío a la suscripción {} (HTTP {})", suscripcion.getId(), resultado.status());
            }
            return resultado.enviado();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("No se pudo enviar la push a la suscripción {}: {}", suscripcion.getId(), e.getMessage());
            return false;
        }
    }

    private String mensajeDe(NotificacionResponse notificacion) {
        // Mismo tag que usa el front para "leída": si la cuota pasa de por vencer a vencida, reemplaza a la anterior
        return mensaje(notificacion.getTitulo(), notificacion.getMensaje(), notificacion.getId());
    }

    private String mensaje(String titulo, String texto, String tag) {
        return jsonMapper.writeValueAsString(Map.of(
                "titulo", titulo, "mensaje", texto, "tag", tag, "url", URL_NOTIFICACIONES));
    }
}
