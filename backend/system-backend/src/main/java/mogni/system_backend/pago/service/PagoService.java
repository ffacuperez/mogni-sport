package mogni.system_backend.pago.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.pago.dto.HistorialPagoResponse;
import mogni.system_backend.pago.dto.PagoResponse;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.dto.RenovacionResponse;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;

/**
 * Renovación (MOGNI-45) con recargo por mora (MOGNI-80) e historial de pagos (MOGNI-48). Una renovación crea la cuota
 * del período siguiente y su único pago, en la misma transacción (reglas C1, C3 y C6).
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final AlumnoRepository alumnoRepository;
    private final MembresiaRepository membresiaRepository;
    private final InscripcionRepository inscripcionRepository;

    public PagoService(PagoRepository pagoRepository, CuotaRepository cuotaRepository,
                        AlumnoRepository alumnoRepository, MembresiaRepository membresiaRepository,
                        InscripcionRepository inscripcionRepository) {
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
        this.alumnoRepository = alumnoRepository;
        this.membresiaRepository = membresiaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * Cálculo de una renovación: lo usan la vista previa y el registro, así nunca muestran cosas distintas.
     * {@code membresia} es con la que renueva y {@code actual} la que usa hoy; si son distintas es un cambio.
     */
    record Renovacion(boolean puedeRenovar, String motivo, SituacionPago situacion, Membresia membresia,
                      Membresia actual, LocalDate inicio, BigDecimal precio, BigDecimal recargo, LocalDate hoy) {

        static Renovacion rechazada(String motivo, SituacionPago situacion, Membresia membresia) {
            return new Renovacion(false, motivo, situacion, membresia, membresia, null, null, null, null);
        }

        LocalDate vencimiento() {
            return inicio != null ? ReglasCuota.vencimientoDesde(inicio) : null;
        }

        BigDecimal total() {
            return precio != null ? precio.add(recargo) : null;
        }

        boolean cambiaDeMembresia() {
            return membresia != null && actual != null && !Objects.equals(membresia.getId(), actual.getId());
        }

        /** Con cambio: si el período empieza en el futuro (pagó por adelantado), la membresía nueva queda programada. */
        boolean programada() {
            return cambiaDeMembresia() && inicio != null && inicio.isAfter(hoy);
        }

        /** Con cambio: desde qué día rige la membresía nueva (hoy si está vencida o en mora). */
        LocalDate rigeDesde() {
            if (!cambiaDeMembresia() || inicio == null) {
                return null;
            }
            return programada() ? inicio : hoy;
        }
    }

    /** Renovación con la membresía actual. */
    static Renovacion calcularRenovacion(Alumno alumno, LocalDate hoy) {
        return calcularRenovacion(alumno, null, hoy);
    }

    /** Renovación con {@code elegida} (null = la actual). El recargo, si está en mora, es sobre el precio de la elegida. */
    static Renovacion calcularRenovacion(Alumno alumno, Membresia elegida, LocalDate hoy) {
        if (!alumno.getCuenta().isActiva()) {
            return Renovacion.rechazada("No se pueden registrar pagos a un alumno inactivo", null, null);
        }
        Optional<Inscripcion> activa = alumno.buscarInscripcionActiva();
        if (activa.isEmpty()) {
            return Renovacion.rechazada("El alumno no tiene una membresía activa", null, null);
        }
        Membresia actual = activa.get().getMembresia();
        Membresia membresia = elegida != null ? elegida : actual;
        BigDecimal precio = BigDecimal.valueOf(membresia.getPrecioActual()).setScale(2);
        BigDecimal sinRecargo = BigDecimal.ZERO.setScale(2);

        // Hasta cuándo pagó: también la cuota de una programada, así no puede pagar dos veces el mismo mes
        Optional<Cuota> ultima = alumno.buscarUltimaCuotaPagada();
        if (ultima.isEmpty()) {
            // Solo con datos viejos (inscripción sin cuotas): el período empieza hoy, como en el alta
            return new Renovacion(true, null, null, membresia, actual, hoy, precio, sinRecargo, hoy);
        }
        LocalDate vencimiento = ultima.get().getFechaVencimiento();
        SituacionPago situacion = SituacionPago.de(vencimiento, hoy);
        if (!ReglasCuota.puedeRenovar(vencimiento, hoy)) {
            String desde = vencimiento.minusDays(ReglasCuota.DIAS_VENTANA_RENOVACION).format(FECHA);
            return Renovacion.rechazada("Todavía no se puede registrar el pago: se habilita "
                    + ReglasCuota.DIAS_VENTANA_RENOVACION + " días antes del vencimiento (desde el " + desde + ")",
                    situacion, actual);
        }
        BigDecimal recargo = situacion == SituacionPago.EN_MORA ? ReglasCuota.recargo(precio) : sinRecargo;
        return new Renovacion(true, null, situacion, membresia, actual, ReglasCuota.inicioRenovacion(vencimiento, hoy),
                precio, recargo, hoy);
    }

    /** Vista previa de la renovación (para el modal de confirmación del front), con la actual o con otra membresía. */
    @Transactional(readOnly = true)
    public RenovacionResponse previsualizarRenovacion(Long alumnoId, Long membresiaId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId));
        Renovacion renovacion = calcularRenovacion(alumno, buscarMembresia(membresiaId), ZonaHoraria.hoy());
        return new RenovacionResponse(alumno.getId(), alumno.getNombre(), alumno.getApellido(), alumno.getDni(),
                renovacion.puedeRenovar(), renovacion.motivo(), renovacion.situacion(),
                renovacion.membresia() != null ? renovacion.membresia().getNombre() : null,
                renovacion.inicio(), renovacion.vencimiento(), renovacion.precio(), renovacion.recargo(),
                renovacion.total(),
                renovacion.actual() != null ? renovacion.actual().getNombre() : null,
                renovacion.cambiaDeMembresia(), renovacion.rigeDesde(), renovacion.programada());
    }

    /**
     * Renueva al alumno: crea la cuota del período siguiente y su pago (fecha de hoy, precio vigente y recargo si
     * está en mora). Con otra membresía la cuota va en una inscripción nueva: PROGRAMADA si el período empieza
     * en el futuro (la activa sigue hasta ese día y la activa ponerAlDia()), o ACTIVA desde hoy si está vencida o en
     * mora (la actual termina hoy). La fila del alumno queda bloqueada hasta el fin de la transacción, así dos
     * registros simultáneos (doble clic) se hacen uno detrás de otro y el segundo ya no está dentro de la ventana.
     */
    @Transactional
    public PagoResponse registrarPago(RegistrarPagoRequest request) {
        LocalDate hoy = ZonaHoraria.hoy();
        Alumno alumno = alumnoRepository.findByIdConBloqueo(request.getAlumnoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + request.getAlumnoId()));

        Renovacion renovacion = calcularRenovacion(alumno, buscarMembresia(request.getMembresiaId()), hoy);
        if (!renovacion.puedeRenovar()) {
            log.warn("Pago rechazado para el alumno {}: {}", alumno.getId(), renovacion.motivo());
            throw new RegistroInvalidoException(renovacion.motivo());
        }

        Inscripcion activa = alumno.buscarInscripcionActiva().orElseThrow();
        Cuota cuota;
        if (!renovacion.cambiaDeMembresia()) {
            cuota = cuotaRepository.save(activa.agregarCuota(renovacion.inicio()));
        } else {
            Inscripcion nueva;
            if (renovacion.programada()) {
                nueva = Inscripcion.nueva(alumno, renovacion.membresia(), renovacion.inicio(), EstadoInscripcion.PROGRAMADA);
            } else {
                activa.cancelar(hoy);
                nueva = Inscripcion.nueva(alumno, renovacion.membresia(), hoy, EstadoInscripcion.ACTIVA);
            }
            cuota = nueva.agregarCuota(renovacion.inicio());
            // Inscripción nueva: save hace persist (no merge), así la cuota guardada es este mismo objeto (ver cuotaGuardada en AlumnoService)
            inscripcionRepository.save(nueva);
            log.info("Renovación del alumno {} con cambio de {} a {}, {} desde el {}", alumno.getId(),
                    renovacion.actual().getNombre(), renovacion.membresia().getNombre(),
                    renovacion.programada() ? "programada" : "activa", renovacion.rigeDesde());
        }
        Pago pago = pagoRepository.save(Pago.de(cuota, renovacion.recargo(), request.getMedioDePago(), hoy));
        log.info("Renovación del alumno {}: cuota {} del {} al {}, pago {} por {} (recargo {}) en {}", alumno.getId(),
                cuota.getId(), cuota.getFechaInicio(), cuota.getFechaVencimiento(), pago.getId(), pago.getMonto(),
                pago.getRecargo(), pago.getMedioDePago());
        return mapearPago(pago);
    }

    /** La membresía elegida al renovar; null si no se eligió (renueva con la actual). */
    private Membresia buscarMembresia(Long membresiaId) {
        if (membresiaId == null) {
            return null;
        }
        return membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new RegistroInvalidoException("La membresía indicada no existe"));
    }

    /** Historial de pagos de un alumno, del más reciente al más antiguo (HU-48). */
    @Transactional(readOnly = true)
    public List<HistorialPagoResponse> listarHistorialDePagos(Long alumnoId) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId);
        }
        List<Pago> pagos = pagoRepository.buscarPorAlumno(alumnoId);
        // La cuota de un cambio de membresía termina el mismo día que la del período que completa (y es posterior)
        Map<LocalDate, Long> primeraCuotaPorVencimiento = pagos.stream()
                .map(Pago::getCuota)
                .collect(Collectors.toMap(Cuota::getFechaVencimiento, Cuota::getId, Math::min));
        return pagos.stream()
                .map(p -> {
                    Cuota cuota = p.getCuota();
                    boolean cambioDeMembresia = !cuota.getId().equals(primeraCuotaPorVencimiento.get(cuota.getFechaVencimiento()));
                    return new HistorialPagoResponse(p.getId(), p.getFechaPago(), p.getMedioDePago(),
                            cuota.getFechaInicio(), cuota.getFechaVencimiento(), cuota.membresiaCubierta().getNombre(),
                            cuota.getPrecio(), p.getRecargo(), p.getMonto(), cambioDeMembresia);
                })
                .toList();
    }

    static PagoResponse mapearPago(Pago pago) {
        Cuota cuota = pago.getCuota();
        return new PagoResponse(pago.getId(), cuota.getId(), cuota.getInscripcion().getAlumno().getId(),
                cuota.membresiaCubierta().getNombre(), cuota.getFechaInicio(), cuota.getFechaVencimiento(),
                cuota.getPrecio(), pago.getRecargo(), pago.getMonto(), pago.getMedioDePago(), pago.getFechaPago());
    }
}
