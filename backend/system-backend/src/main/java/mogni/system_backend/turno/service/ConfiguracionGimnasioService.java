package mogni.system_backend.turno.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.ConfiguracionGimnasioResponse;
import mogni.system_backend.turno.entity.ConfiguracionGimnasio;
import mogni.system_backend.turno.repository.ConfiguracionGimnasioRepository;

/**
 * HU-252: cupo máximo de alumnos por turno, único para todo el gimnasio. Cambiarlo se aplica (después de confirmar)
 * solo a los turnos futuros sin reservas; los que ya tienen reservas conservan el cupo con el que se generaron.
 */
@Service
public class ConfiguracionGimnasioService {

    private final ConfiguracionGimnasioRepository configuracionGimnasioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ConfiguracionGimnasioService(ConfiguracionGimnasioRepository configuracionGimnasioRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.configuracionGimnasioRepository = configuracionGimnasioRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public ConfiguracionGimnasioResponse obtenerConfiguracion() {
        return new ConfiguracionGimnasioResponse(buscarConfiguracion().getCupoMaximo());
    }

    /** Cupo vigente, el que copian los turnos al generarse. */
    @Transactional(readOnly = true)
    public int obtenerCupoMaximo() {
        return buscarConfiguracion().getCupoMaximo();
    }

    @Transactional
    public ConfiguracionGimnasioResponse cambiarCupoMaximo(Integer cupoMaximo) {
        if (cupoMaximo == null || cupoMaximo <= 0) {
            throw new RegistroInvalidoException("El cupo máximo tiene que ser un número entero mayor a 0.");
        }
        ConfiguracionGimnasio configuracion = buscarConfiguracion();
        configuracion.setCupoMaximo(cupoMaximo);
        int cupoGuardado = configuracionGimnasioRepository.save(configuracion).getCupoMaximo();
        eventPublisher.publishEvent(new CupoModificadoEvent(cupoGuardado));
        return new ConfiguracionGimnasioResponse(cupoGuardado);
    }

    private ConfiguracionGimnasio buscarConfiguracion() {
        // La fila la crea InicializacionConfiguracionGimnasio al arrancar
        return configuracionGimnasioRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Falta la configuración del gimnasio"));
    }
}
