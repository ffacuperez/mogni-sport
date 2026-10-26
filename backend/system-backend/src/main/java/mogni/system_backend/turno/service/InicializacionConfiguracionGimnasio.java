package mogni.system_backend.turno.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.turno.entity.ConfiguracionGimnasio;
import mogni.system_backend.turno.repository.ConfiguracionGimnasioRepository;

/** Al arrancar: si todavía no existe la configuración del gimnasio, la crea con el cupo inicial (HU-252). */
@Component
@Order(3)
public class InicializacionConfiguracionGimnasio implements ApplicationRunner {

    public static final int CUPO_MAXIMO_INICIAL = 5;

    private static final Logger log = LoggerFactory.getLogger(InicializacionConfiguracionGimnasio.class);

    private final ConfiguracionGimnasioRepository configuracionGimnasioRepository;

    public InicializacionConfiguracionGimnasio(ConfiguracionGimnasioRepository configuracionGimnasioRepository) {
        this.configuracionGimnasioRepository = configuracionGimnasioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configuracionGimnasioRepository.count() == 0) {
            configuracionGimnasioRepository.save(new ConfiguracionGimnasio(null, CUPO_MAXIMO_INICIAL));
            log.info("Se creó la configuración del gimnasio con cupo máximo {}", CUPO_MAXIMO_INICIAL);
        }
    }
}
