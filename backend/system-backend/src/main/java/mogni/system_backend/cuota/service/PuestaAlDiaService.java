package mogni.system_backend.cuota.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.service.BajaAlumnoService;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

/**
 * "Gestionar alumnos en mora" (rediseño de cuotas, sección 13). La situación de pago se calcula, así que lo único
 * que cambia datos con el paso del tiempo está acá:
 * <ol>
 *   <li>cancela los turnos que todavía no empezaron de los alumnos EN_MORA (el lugar se libera solo, porque los
 *       lugares se cuentan con las reservas RESERVADA);</li>
 *   <li>da de baja a los que llevan 7 días o más en mora, igual que la baja manual.</li>
 * </ol>
 * Es idempotente y tiene su propia transacción: los que la usan con una consulta readOnly la llaman antes, desde el
 * controlador. Se llama al listar alumnos y cuotas, al consultar la agenda, los turnos disponibles y al reservar, y
 * desde el cron de las 01:00 (respaldo por si nadie usa la app). No se llama al iniciar sesión.
 */
@Service
public class PuestaAlDiaService {

    private static final Logger log = LoggerFactory.getLogger(PuestaAlDiaService.class);

    private final CuotaRepository cuotaRepository;
    private final CancelacionPorSistemaService cancelacionPorSistemaService;
    private final BajaAlumnoService bajaAlumnoService;

    public PuestaAlDiaService(CuotaRepository cuotaRepository,
                              CancelacionPorSistemaService cancelacionPorSistemaService,
                              BajaAlumnoService bajaAlumnoService) {
        this.cuotaRepository = cuotaRepository;
        this.cancelacionPorSistemaService = cancelacionPorSistemaService;
        this.bajaAlumnoService = bajaAlumnoService;
    }

    @Transactional
    public void ponerAlDia() {
        ponerAlDia(ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    void ponerAlDia(LocalDateTime ahora) {
        int reservasCanceladas = 0;
        int bajas = 0;
        for (Cuota cuota : cuotaRepository.buscarUltimasCuotas().values()) {
            if (SituacionPago.de(cuota, ahora.toLocalDate()) != SituacionPago.EN_MORA) {
                continue;
            }
            Alumno alumno = cuota.getInscripcion().getAlumno();
            if (ReglasCuota.diasEnMora(cuota.getFechaVencimiento(), ahora.toLocalDate())
                    >= ReglasCuota.DIAS_MORA_BAJA_AUTOMATICA) {
                bajaAlumnoService.darDeBaja(alumno, ahora);
                bajas++;
                log.info("Baja automática del alumno {}: su cuota venció el {}", alumno.getId(), cuota.getFechaVencimiento());
            } else {
                reservasCanceladas += cancelacionPorSistemaService.cancelarReservasFuturas(alumno.getId(), ahora);
            }
        }
        if (reservasCanceladas > 0 || bajas > 0) {
            log.info("Puesta al día: {} reservas canceladas por mora, {} bajas automáticas", reservasCanceladas, bajas);
        }
    }
}
