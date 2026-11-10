package mogni.system_backend.panel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.panel.dto.PanelResponse;
import mogni.system_backend.turno.entity.Asistencia;
import mogni.system_backend.turno.entity.EstadoReserva;
import mogni.system_backend.turno.entity.EstadoTurno;
import mogni.system_backend.turno.repository.ReservaRepository;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * Panel del propietario: el resumen del mes calendario en curso (zona del gimnasio). Todo sale de consultas agregadas
 * en la base; la única regla de negocio que usa, la situación de pago, es la misma de la lista de Alumnos.
 */
@Service
public class PanelService {

    /** Meses del gráfico de ingresos, incluido el actual. Pendiente de confirmar si son 6 o 12: se cambia solo acá. */
    public static final int MESES_GRAFICO = 6;

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PagoRepository pagoRepository;
    private final AlumnoRepository alumnoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CuotaRepository cuotaRepository;
    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;

    public PanelService(PagoRepository pagoRepository, AlumnoRepository alumnoRepository,
                        InscripcionRepository inscripcionRepository, CuotaRepository cuotaRepository,
                        TurnoRepository turnoRepository, ReservaRepository reservaRepository) {
        this.pagoRepository = pagoRepository;
        this.alumnoRepository = alumnoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cuotaRepository = cuotaRepository;
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public PanelResponse consultar() {
        return consultar(ZonaHoraria.ahora());
    }

    /** Igual que el anterior, con "ahora" como parámetro para poder probarlo con cualquier fecha y hora. */
    @Transactional(readOnly = true)
    public PanelResponse consultar(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        YearMonth mes = YearMonth.from(hoy);
        LocalDate inicioMes = mes.atDay(1);
        LocalDate finMes = mes.atEndOfMonth();

        List<PanelResponse.IngresoMes> porMes = ingresosPorMes(mes);
        BigDecimal ingresosMes = porMes.get(porMes.size() - 1).total();
        BigDecimal ingresosMesAnterior = porMes.get(porMes.size() - 2).total();

        Map<MedioDePago, PagoRepository.IngresoPorMedio> porMedio = pagoRepository
                .sumarPorMedioDePago(inicioMes, finMes).stream()
                .collect(Collectors.toMap(PagoRepository.IngresoPorMedio::getMedio, fila -> fila));
        BigDecimal recargos = porMedio.values().stream()
                .map(PagoRepository.IngresoPorMedio::getRecargos)
                .reduce(CERO, BigDecimal::add);

        PanelResponse.Ingresos ingresos = new PanelResponse.Ingresos(
                ingresosMes,
                ingresosMesAnterior,
                porcentaje(ingresosMes.subtract(ingresosMesAnterior), ingresosMesAnterior),
                totalDe(porMedio.get(MedioDePago.EFECTIVO)),
                totalDe(porMedio.get(MedioDePago.TRANSFERENCIA)),
                escala(recargos));

        long activos = alumnoRepository.countByCuentaActiva(true);
        PanelResponse.Alumnos alumnos = new PanelResponse.Alumnos(
                activos,
                alumnoRepository.contarNuevos(inicioMes, finMes),
                alumnoRepository.contarBajas(inicioMes, finMes),
                contarEnMora(hoy));

        long reservas = reservaRepository.contarEnTurnosComenzados(EstadoReserva.RESERVADA, EstadoTurno.DISPONIBLE,
                inicioMes, hoy, ahora.toLocalTime());
        long cupo = turnoRepository.sumarCupoDeTurnosComenzados(EstadoTurno.DISPONIBLE, inicioMes, hoy,
                ahora.toLocalTime());

        Map<Asistencia, Long> porAsistencia = reservaRepository
                .contarPorAsistencia(EstadoReserva.RESERVADA, inicioMes, hoy).stream()
                .collect(Collectors.toMap(ReservaRepository.ReservasPorAsistencia::getAsistencia,
                        ReservaRepository.ReservasPorAsistencia::getCantidad));
        long presentes = porAsistencia.getOrDefault(Asistencia.PRESENTE, 0L);
        long ausentes = porAsistencia.getOrDefault(Asistencia.AUSENTE, 0L);

        return new PanelResponse(
                hoy,
                ingresos,
                porMes,
                alumnos,
                activosPorMembresia(activos),
                new PanelResponse.Ocupacion(reservas, cupo, porcentaje(BigDecimal.valueOf(reservas), BigDecimal.valueOf(cupo))),
                new PanelResponse.Asistencia(presentes, ausentes,
                        porcentaje(BigDecimal.valueOf(presentes), BigDecimal.valueOf(presentes + ausentes))));
    }

    /** Los últimos MESES_GRAFICO meses hasta el actual, del más viejo al más nuevo; los meses sin pagos en 0. */
    private List<PanelResponse.IngresoMes> ingresosPorMes(YearMonth actual) {
        YearMonth primero = actual.minusMonths(MESES_GRAFICO - 1L);
        Map<YearMonth, BigDecimal> cobrado = pagoRepository
                .sumarPorMes(primero.atDay(1), actual.atEndOfMonth()).stream()
                .collect(Collectors.toMap(fila -> YearMonth.of(fila.getAnio(), fila.getMes()),
                        PagoRepository.IngresoMensual::getTotal));
        List<PanelResponse.IngresoMes> meses = new ArrayList<>();
        for (YearMonth m = primero; !m.isAfter(actual); m = m.plusMonths(1)) {
            meses.add(new PanelResponse.IngresoMes(m.getYear(), m.getMonthValue(), escala(cobrado.get(m))));
        }
        return meses;
    }

    /** La misma cuenta que los recuadros de la lista de Alumnos: última cuota pagada de cada alumno activo. */
    private long contarEnMora(LocalDate hoy) {
        return cuotaRepository.buscarUltimasCuotas().values().stream()
                .filter(cuota -> SituacionPago.de(cuota, hoy) == SituacionPago.EN_MORA)
                .count();
    }

    /** Por membresía; si algún alumno activo no tiene inscripción ACTIVA, va en una fila sin nombre para que sumen. */
    private List<PanelResponse.ActivosMembresia> activosPorMembresia(long activos) {
        List<PanelResponse.ActivosMembresia> filas = new ArrayList<>(inscripcionRepository
                .contarAlumnosActivosPorMembresia(EstadoInscripcion.ACTIVA).stream()
                .map(fila -> new PanelResponse.ActivosMembresia(fila.getMembresia(), fila.getCantidad()))
                .toList());
        long conMembresia = filas.stream().mapToLong(PanelResponse.ActivosMembresia::cantidad).sum();
        if (activos > conMembresia) {
            filas.add(new PanelResponse.ActivosMembresia(null, activos - conMembresia));
        }
        return filas;
    }

    private static BigDecimal totalDe(PagoRepository.IngresoPorMedio fila) {
        return fila == null ? CERO : escala(fila.getTotal());
    }

    private static BigDecimal escala(BigDecimal monto) {
        return monto == null ? CERO : monto.setScale(2, RoundingMode.HALF_UP);
    }

    /** parte / total en porcentaje con un decimal; null si el total es 0 (se muestra "—"). */
    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return null;
        }
        return parte.multiply(CIEN).divide(total, 1, RoundingMode.HALF_UP);
    }
}
