package mogni.system_backend.pago.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
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

    public PagoService(PagoRepository pagoRepository, CuotaRepository cuotaRepository,
                        AlumnoRepository alumnoRepository) {
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
        this.alumnoRepository = alumnoRepository;
    }

    /** Cálculo de una renovación: lo usan la vista previa y el registro, así nunca muestran cosas distintas. */
    record Renovacion(boolean puedeRenovar, String motivo, SituacionPago situacion, Membresia membresia,
                      LocalDate inicio, BigDecimal precio, BigDecimal recargo) {

        static Renovacion rechazada(String motivo, SituacionPago situacion, Membresia membresia) {
            return new Renovacion(false, motivo, situacion, membresia, null, null, null);
        }

        LocalDate vencimiento() {
            return inicio != null ? inicio.plusDays(ReglasCuota.DIAS_CUOTA) : null;
        }

        BigDecimal total() {
            return precio != null ? precio.add(recargo) : null;
        }
    }

    static Renovacion calcularRenovacion(Alumno alumno, LocalDate hoy) {
        if (!alumno.getCuenta().isActiva()) {
            return Renovacion.rechazada("No se pueden registrar pagos a un alumno inactivo", null, null);
        }
        Optional<Inscripcion> activa = alumno.buscarInscripcionActiva();
        if (activa.isEmpty()) {
            return Renovacion.rechazada("El alumno no tiene una membresía activa", null, null);
        }
        Membresia membresia = activa.get().getMembresia();
        BigDecimal precio = BigDecimal.valueOf(membresia.getPrecioActual()).setScale(2);
        BigDecimal sinRecargo = BigDecimal.ZERO.setScale(2);

        Optional<Cuota> ultima = activa.get().buscarUltimaCuota();
        if (ultima.isEmpty()) {
            // Solo con datos viejos (inscripción sin cuotas): el período empieza hoy, como en el alta
            return new Renovacion(true, null, null, membresia, hoy, precio, sinRecargo);
        }
        LocalDate vencimiento = ultima.get().getFechaVencimiento();
        SituacionPago situacion = SituacionPago.de(vencimiento, hoy);
        if (!ReglasCuota.puedeRenovar(vencimiento, hoy)) {
            String desde = vencimiento.minusDays(ReglasCuota.DIAS_VENTANA_RENOVACION).format(FECHA);
            return Renovacion.rechazada("Todavía no se puede registrar el pago: se habilita "
                    + ReglasCuota.DIAS_VENTANA_RENOVACION + " días antes del vencimiento (desde el " + desde + ")",
                    situacion, membresia);
        }
        BigDecimal recargo = situacion == SituacionPago.EN_MORA ? ReglasCuota.recargo(precio) : sinRecargo;
        return new Renovacion(true, null, situacion, membresia, ReglasCuota.inicioRenovacion(vencimiento, hoy),
                precio, recargo);
    }

    /** Vista previa de la renovación (para el modal de confirmación del front). */
    @Transactional(readOnly = true)
    public RenovacionResponse previsualizarRenovacion(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId));
        Renovacion renovacion = calcularRenovacion(alumno, ZonaHoraria.hoy());
        return new RenovacionResponse(alumno.getId(), alumno.getNombre(), alumno.getApellido(), alumno.getDni(),
                renovacion.puedeRenovar(), renovacion.motivo(), renovacion.situacion(),
                renovacion.membresia() != null ? renovacion.membresia().getNombre() : null,
                renovacion.inicio(), renovacion.vencimiento(), renovacion.precio(), renovacion.recargo(),
                renovacion.total());
    }

    /**
     * Renueva al alumno: crea la cuota del período siguiente y su pago (fecha de hoy, precio vigente y recargo si
     * está en mora). La fila del alumno queda bloqueada hasta el fin de la transacción, así dos registros simultáneos
     * (doble clic) se hacen uno detrás de otro y el segundo ya no está dentro de la ventana de renovación.
     */
    @Transactional
    public PagoResponse registrarPago(RegistrarPagoRequest request) {
        LocalDate hoy = ZonaHoraria.hoy();
        Alumno alumno = alumnoRepository.findByIdConBloqueo(request.getAlumnoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + request.getAlumnoId()));

        Renovacion renovacion = calcularRenovacion(alumno, hoy);
        if (!renovacion.puedeRenovar()) {
            log.warn("Pago rechazado para el alumno {}: {}", alumno.getId(), renovacion.motivo());
            throw new RegistroInvalidoException(renovacion.motivo());
        }

        Inscripcion inscripcion = alumno.buscarInscripcionActiva().orElseThrow();
        Cuota cuota = cuotaRepository.save(inscripcion.agregarCuota(renovacion.inicio()));
        Pago pago = pagoRepository.save(Pago.de(cuota, renovacion.recargo(), request.getMedioDePago(), hoy));
        log.info("Renovación del alumno {}: cuota {} del {} al {}, pago {} por {} (recargo {}) en {}", alumno.getId(),
                cuota.getId(), cuota.getFechaInicio(), cuota.getFechaVencimiento(), pago.getId(), pago.getMonto(),
                pago.getRecargo(), pago.getMedioDePago());
        return mapearPago(pago);
    }

    /** Historial de pagos de un alumno, del más reciente al más antiguo (HU-48). */
    @Transactional(readOnly = true)
    public List<HistorialPagoResponse> listarHistorialDePagos(Long alumnoId) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new RecursoNoEncontradoException("No existe un alumno con id " + alumnoId);
        }
        return pagoRepository.buscarPorAlumno(alumnoId).stream()
                .map(p -> {
                    Cuota cuota = p.getCuota();
                    return new HistorialPagoResponse(p.getId(), p.getFechaPago(), p.getMedioDePago(),
                            cuota.getFechaInicio(), cuota.getFechaVencimiento(), cuota.membresiaCubierta().getNombre(),
                            cuota.getPrecio(), p.recargoOCero(), p.getMonto());
                })
                .toList();
    }

    static PagoResponse mapearPago(Pago pago) {
        Cuota cuota = pago.getCuota();
        return new PagoResponse(pago.getId(), cuota.getId(), cuota.getInscripcion().getAlumno().getId(),
                cuota.membresiaCubierta().getNombre(), cuota.getFechaInicio(), cuota.getFechaVencimiento(),
                cuota.getPrecio(), pago.recargoOCero(), pago.getMonto(), pago.getMedioDePago(), pago.getFechaPago());
    }
}
