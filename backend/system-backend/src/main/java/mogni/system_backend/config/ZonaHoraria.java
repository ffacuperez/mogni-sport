package mogni.system_backend.config;

import java.time.LocalDate;
import java.time.ZoneId;

/** Zona del gimnasio: el "hoy" de cobros y vencimientos no depende de la zona del servidor. */
public final class ZonaHoraria {

    public static final String ZONA = "America/Argentina/Buenos_Aires";

    private ZonaHoraria() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZoneId.of(ZONA));
    }
}
