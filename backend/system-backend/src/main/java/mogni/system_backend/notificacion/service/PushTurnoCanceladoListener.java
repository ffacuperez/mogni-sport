package mogni.system_backend.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import mogni.system_backend.turno.service.TurnoCanceladoPorGimnasioEvent;

/**
 * Manda la push cuando el gimnasio cancela un turno. Corre DESPUÉS de confirmar la cancelación (si se revierte, no se
 * avisa nada) y en otro hilo, así el propietario no espera a los servicios de push al cancelar un día entero.
 * Un error solo se registra: la cancelación ya está hecha y el aviso sigue en la app.
 */
@Component
public class PushTurnoCanceladoListener {

    private static final Logger log = LoggerFactory.getLogger(PushTurnoCanceladoListener.class);

    private final PushService pushService;

    public PushTurnoCanceladoListener(PushService pushService) {
        this.pushService = pushService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelarseUnTurno(TurnoCanceladoPorGimnasioEvent evento) {
        try {
            // Mismo id que la notificación de la app (ver NotificacionService): la push y el aviso son el mismo
            pushService.enviarAviso(evento.cuentaId(), "turno-" + evento.avisoId(), evento.titulo(), evento.mensaje());
        } catch (RuntimeException e) {
            log.warn("No se pudo mandar la push del aviso de turno {}", evento.avisoId(), e);
        }
    }
}
