package mogni.system_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async: las push de turnos cancelados se mandan en otro hilo (ver PushTurnoCanceladoListener). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
