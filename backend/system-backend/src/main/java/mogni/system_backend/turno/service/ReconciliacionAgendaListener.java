package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * Reconcilia los turnos ya generados cuando el propietario cambia las franjas o el cupo. Corre DESPUÉS de confirmar
 * la edición (si no, la transacción de cada fecha no vería la franja nueva). Un error no revierte ni rompe la edición:
 * se registra y la red de seguridad de GeneracionTurnosService lo corrige en la próxima consulta.
 */
@Component
public class ReconciliacionAgendaListener {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacionAgendaListener.class);

    private final TurnoRepository turnoRepository;
    private final ReconciliacionTurnosService reconciliacionTurnosService;

    public ReconciliacionAgendaListener(TurnoRepository turnoRepository,
                                        ReconciliacionTurnosService reconciliacionTurnosService) {
        this.turnoRepository = turnoRepository;
        this.reconciliacionTurnosService = reconciliacionTurnosService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alModificarseLasFranjas(FranjasModificadasEvent evento) {
        LocalDateTime ahora = ZonaHoraria.ahora();
        for (LocalDate fecha : fechasAReconciliar(ahora)) {
            try {
                reconciliacionTurnosService.reconciliarFecha(fecha, ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudieron reconciliar los turnos del {} con las franjas; se reintenta en la próxima consulta",
                        fecha, e);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarElCupo(CupoModificadoEvent evento) {
        LocalDateTime ahora = ZonaHoraria.ahora();
        for (LocalDate fecha : fechasAReconciliar(ahora)) {
            try {
                reconciliacionTurnosService.aplicarCupoEnFecha(fecha, evento.cupoMaximo(), ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudo aplicar el cupo {} a los turnos del {}", evento.cupoMaximo(), fecha, e);
            }
        }
    }

    /** Fechas de hoy en adelante que ya tienen turnos generados. */
    private List<LocalDate> fechasAReconciliar(LocalDateTime ahora) {
        return turnoRepository.findFechasConTurnosDesde(ahora.toLocalDate());
    }
}
