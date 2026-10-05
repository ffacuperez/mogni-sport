package mogni.system_backend.inscripcion.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;

/**
 * Marca como VENCIDA cada inscripción ACTIVA cuya fecha de fin ya pasó.
 * Corre todos los días a las 00:05 (hora de Argentina) y también al arrancar la aplicación,
 * para ponerse al día si el servidor estuvo apagado.
 */
@Service
public class VencimientoInscripcionesService {

    static final String ZONA = "America/Argentina/Buenos_Aires";

    private static final Logger log = LoggerFactory.getLogger(VencimientoInscripcionesService.class);

    private final InscripcionRepository inscripcionRepository;

    public VencimientoInscripcionesService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = ZONA)
    public int marcarInscripcionesVencidas() {
        LocalDate hoy = LocalDate.now(ZoneId.of(ZONA));
        int vencidas = inscripcionRepository.marcarVencidas(hoy, EstadoInscripcion.ACTIVA, EstadoInscripcion.VENCIDA);
        if (vencidas > 0) {
            log.info("Se marcaron {} inscripciones como VENCIDA", vencidas);
        }
        return vencidas;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        marcarInscripcionesVencidas();
    }
}
