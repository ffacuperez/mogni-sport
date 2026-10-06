package mogni.system_backend.cuota.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;

/**
 * Al arrancar: genera la primera cuota de las inscripciones ACTIVAS que no tienen ninguna
 * (las creadas antes de HU-45) y después marca las cuotas atrasadas.
 * Corre después de {@link mogni.system_backend.inscripcion.service.MigracionInscripciones}.
 */
@Component
@Order(2)
public class InicializacionCuotas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacionCuotas.class);

    private final InscripcionRepository inscripcionRepository;
    private final CuotaRepository cuotaRepository;
    private final VencimientoCuotasService vencimientoCuotasService;

    public InicializacionCuotas(InscripcionRepository inscripcionRepository,
                                CuotaRepository cuotaRepository,
                                VencimientoCuotasService vencimientoCuotasService) {
        this.inscripcionRepository = inscripcionRepository;
        this.cuotaRepository = cuotaRepository;
        this.vencimientoCuotasService = vencimientoCuotasService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        generarCuotasFaltantes();
        vencimientoCuotasService.marcarCuotasAtrasadas();
    }

    public int generarCuotasFaltantes() {
        List<Inscripcion> sinCuotas = inscripcionRepository.findSinCuotas(EstadoInscripcion.ACTIVA);
        cuotaRepository.saveAll(sinCuotas.stream().map(Cuota::primeraDe).toList());
        if (!sinCuotas.isEmpty()) {
            log.info("Se generó la primera cuota de {} inscripciones activas que no tenían", sinCuotas.size());
        }
        return sinCuotas.size();
    }
}
