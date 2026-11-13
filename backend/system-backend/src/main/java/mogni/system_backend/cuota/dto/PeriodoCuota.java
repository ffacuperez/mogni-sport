package mogni.system_backend.cuota.dto;

import java.time.LocalDate;

/** Un período pago: desde el inicio de la cuota hasta su vencimiento (el día en que empieza la siguiente). */
public record PeriodoCuota(LocalDate desde, LocalDate hasta) {
}
