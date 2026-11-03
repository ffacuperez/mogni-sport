package mogni.system_backend.cuota.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.plan.service.PlanAsignadoService;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

/**
 * "Gestionar alumnos en mora" (rediseño de cuotas, puesta al día). La situación de pago se calcula, así que lo único
 * que cambia datos con el paso del tiempo está acá, en este orden:
 * <ol>
 *   <li>activa las inscripciones PROGRAMADA cuyo período ya empezó (cambio de membresía pagado por adelantado):
 *       la activa del alumno termina ese día y la programada pasa a ACTIVA;</li>
 *   <li>cancela los turnos que todavía no empezaron de los alumnos EN_MORA (el lugar se libera solo, porque los
 *       lugares se cuentan con las reservas RESERVADA);</li>
 *   <li>HU-266: finaliza los planes de entrenamiento vencidos con su fecha fin y libera la plantilla. Lo que se ve ya
 *       se calcula con la fecha; esto solo deja prolijos la base y el contador de la plantilla.</li>
 * </ol>
 * No hay baja automática: la baja de un alumno en mora la hace el propietario a mano (decidido 24/10).
 * Es idempotente y tiene su propia transacción: los que la usan con una consulta readOnly la llaman antes, desde el
 * controlador. Se llama al listar alumnos y cuotas, al consultar la agenda, los turnos disponibles y al reservar, y
 * desde el cron de las 01:00 (respaldo por si nadie usa la app). No se llama al iniciar sesión.
 */
@Service
public class PuestaAlDiaService {

    private static final Logger log = LoggerFactory.getLogger(PuestaAlDiaService.class);

    private final CuotaRepository cuotaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CancelacionPorSistemaService cancelacionPorSistemaService;
    private final PlanAsignadoService planAsignadoService;

    public PuestaAlDiaService(CuotaRepository cuotaRepository, InscripcionRepository inscripcionRepository,
                              CancelacionPorSistemaService cancelacionPorSistemaService,
                              PlanAsignadoService planAsignadoService) {
        this.cuotaRepository = cuotaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cancelacionPorSistemaService = cancelacionPorSistemaService;
        this.planAsignadoService = planAsignadoService;
    }

    @Transactional
    public void ponerAlDia() {
        ponerAlDia(ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    void ponerAlDia(LocalDateTime ahora) {
        activarProgramadas(ahora.toLocalDate());

        int reservasCanceladas = 0;
        for (Cuota cuota : cuotaRepository.buscarUltimasCuotas().values()) {
            if (SituacionPago.de(cuota, ahora.toLocalDate()) == SituacionPago.EN_MORA) {
                Long alumnoId = cuota.getInscripcion().getAlumno().getId();
                reservasCanceladas += cancelacionPorSistemaService.cancelarReservasFuturas(alumnoId, ahora);
            }
        }
        if (reservasCanceladas > 0) {
            log.info("Puesta al día: {} reservas canceladas por mora", reservasCanceladas);
        }

        int planesVencidos = planAsignadoService.cerrarVencidos(ahora.toLocalDate());
        if (planesVencidos > 0) {
            log.info("Puesta al día: {} planes de entrenamiento vencidos quedan finalizados", planesVencidos);
        }
    }

    /**
     * Con "≤ hoy" y no "= hoy": si algún día no corrió (servidor dormido, nadie entró), se activa la próxima vez.
     * La activa termina el día en que empieza la programada, aunque se active después.
     */
    private void activarProgramadas(LocalDate hoy) {
        for (Inscripcion programada : inscripcionRepository.findByEstadoAndFechaInicioLessThanEqual(
                EstadoInscripcion.PROGRAMADA, hoy)) {
            programada.getAlumno().buscarInscripcionActiva()
                    .ifPresent(activa -> activa.cancelar(programada.getFechaInicio()));
            programada.activar();
            log.info("Puesta al día: el alumno {} pasa a {} desde el {}", programada.getAlumno().getId(),
                    programada.getMembresia().getNombre(), programada.getFechaInicio());
        }
    }
}
