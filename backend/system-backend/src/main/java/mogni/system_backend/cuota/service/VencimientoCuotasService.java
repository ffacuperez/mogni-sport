package mogni.system_backend.cuota.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;

/**
 * Marca como ATRASADA cada cuota PENDIENTE cuyo vencimiento ya pasó.
 * Corre todos los días a la 01:00 (hora de Argentina) y también al arrancar la aplicación
 * (ver {@link InicializacionCuotas}), para ponerse al día si el servidor estuvo apagado.
 */
@Service
public class VencimientoCuotasService {

    private static final Logger log = LoggerFactory.getLogger(VencimientoCuotasService.class);

    private final CuotaRepository cuotaRepository;

    public VencimientoCuotasService(CuotaRepository cuotaRepository) {
        this.cuotaRepository = cuotaRepository;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = ZonaHoraria.ZONA)
    public int marcarCuotasAtrasadas() {
        int atrasadas = cuotaRepository.marcarAtrasadas(ZonaHoraria.hoy(), EstadoCuota.PENDIENTE, EstadoCuota.ATRASADA);
        if (atrasadas > 0) {
            log.info("Se marcaron {} cuotas como ATRASADA", atrasadas);
        }
        return atrasadas;
    }
}
