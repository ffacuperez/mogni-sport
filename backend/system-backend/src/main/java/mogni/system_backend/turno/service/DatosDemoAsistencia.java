package mogni.system_backend.turno.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.HorarioTurno;
import mogni.system_backend.turno.entity.Reserva;
import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * SOLO PARA PROBAR EN LA COMPU (perfil "localhost", propiedad app.demo.asistencia=true). Nunca corre en Railway.
 *
 * <p>HU-262: para marcar asistencia hace falta un turno que ya comenzó con alumnos anotados, y los alumnos solo pueden
 * reservar con 1 hora de anticipación (y las franjas son de lunes a viernes). Al arrancar arma, con los alumnos
 * activos de data.sql:
 * <ul>
 *   <li>hoy, el turno de la hora actual (en curso) con un presente, un ausente y el resto sin marcar;</li>
 *   <li>hoy, el turno anterior (ya terminó, se puede seguir corrigiendo) sin marcar;</li>
 *   <li>hoy, el turno siguiente (todavía no comenzó: no deja marcar);</li>
 *   <li>ayer a las 18:00, un presente y el resto sin marcar (al abrir la agenda pasan a «Ausente»).</li>
 * </ul>
 * Si el turno ya existe lo reutiliza, y solo agrega reservas a los turnos que no tienen ninguna.
 */
@Component
@ConditionalOnProperty(name = "app.demo.asistencia", havingValue = "true")
@Order // después de InicializacionConfiguracionGimnasio
public class DatosDemoAsistencia implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoAsistencia.class);
    private static final int ALUMNOS_POR_TURNO = 4;

    private final AlumnoRepository alumnoRepository;
    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;
    private final TransactionTemplate transactionTemplate;

    public DatosDemoAsistencia(AlumnoRepository alumnoRepository, TurnoRepository turnoRepository,
                               ReservaRepository reservaRepository, TransactionTemplate transactionTemplate) {
        this.alumnoRepository = alumnoRepository;
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(estado -> cargar(ZonaHoraria.ahora()));
    }

    private void cargar(LocalDateTime ahora) {
        List<Alumno> alumnos = alumnoRepository.findByCuentaActiva(true).stream()
                .filter(alumno -> alumno.buscarInscripcionActiva().isPresent())
                .limit(ALUMNOS_POR_TURNO)
                .toList();
        if (alumnos.isEmpty()) {
            log.warn("Demo asistencia: no hay alumnos activos. Levantá una vez con el perfil seed para cargar data.sql.");
            return;
        }

        LocalDate hoy = ahora.toLocalDate();
        int hora = ahora.getHour();
        // En curso: un presente, un ausente y el resto sin marcar (se ven los tres contadores)
        reservar(turno(hoy, hora), alumnos, ahora, Asistencia.PRESENTE, Asistencia.AUSENTE);
        if (hora > 0) {
            reservar(turno(hoy, hora - 1), alumnos, ahora);
        }
        if (hora < 23) {
            reservar(turno(hoy, hora + 1), alumnos, ahora);
        }
        reservar(turno(hoy.minusDays(1), 18), alumnos, ahora, Asistencia.PRESENTE);
        log.info("Demo asistencia: turnos de prueba listos para el {} (agenda de hoy y de ayer)", hoy);
    }

    /** El turno de esa fecha y hora (en punto), o uno nuevo con cupo para todos los alumnos de la demo. */
    private Turno turno(LocalDate fecha, int hora) {
        LocalTime inicio = LocalTime.of(hora, 0);
        return turnoRepository.findByFechaOrderByHoraInicioAsc(fecha).stream()
                .filter(turno -> turno.getHoraInicio().equals(inicio))
                .findFirst()
                .orElseGet(() -> turnoRepository.save(
                        new Turno(fecha, new HorarioTurno(inicio, inicio.plusHours(1)), ALUMNOS_POR_TURNO + 2)));
    }

    /** Anota a los alumnos (si el turno no tiene reservas); los primeros reciben las marcas indicadas, en orden. */
    private void reservar(Turno turno, List<Alumno> alumnos, LocalDateTime ahora, Asistencia... marcas) {
        if (reservaRepository.countByTurnoIdAndEstado(turno.getId(), EstadoReserva.RESERVADA) > 0) {
            return;
        }
        for (int i = 0; i < alumnos.size(); i++) {
            Reserva reserva = new Reserva(null, alumnos.get(i), turno, ahora.minusDays(1), EstadoReserva.RESERVADA);
            reserva.setAsistencia(i < marcas.length ? marcas[i] : null);
            reservaRepository.save(reserva);
        }
    }
}
