package mogni.system_backend.cuota.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import mogni.system_backend.config.ZonaHoraria;

/**
 * Respaldo diario de la puesta al día (cancelación de turnos por mora): corre todos los días a la
 * 01:00 (hora de Argentina) aunque nadie use la app. No alcanza sola porque el back puede estar apagado a esa
 * hora (un reinicio o un deploy); por eso {@link PuestaAlDiaService} también se llama al usar las pantallas.
 */
@Service
public class VencimientoCuotasService {

    private final PuestaAlDiaService puestaAlDiaService;

    public VencimientoCuotasService(PuestaAlDiaService puestaAlDiaService) {
        this.puestaAlDiaService = puestaAlDiaService;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = ZonaHoraria.ZONA)
    public void ponerAlDia() {
        puestaAlDiaService.ponerAlDia();
    }
}
