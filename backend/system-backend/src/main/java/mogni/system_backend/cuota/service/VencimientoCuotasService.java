package mogni.system_backend.cuota.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import mogni.system_backend.config.ZonaHoraria;

/**
 * Respaldo diario de la puesta al día (cancelación de turnos por mora y baja automática): corre todos los días a la
 * 01:00 (hora de Argentina) aunque nadie use la app. No alcanza sola porque en Railway el servidor puede estar
 * dormido a esa hora; por eso {@link PuestaAlDiaService} también se llama al usar las pantallas.
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
