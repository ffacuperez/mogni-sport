package mogni.system_backend.cuota.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.dto.EstadoCuota;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

@Service
public class CuotaService {

    /** Primero en mora, después vencidas, después el resto por vencimiento más cercano; sin cuotas al final. */
    private static final Comparator<CuotaResponse> ORDEN = Comparator
            .comparingInt((CuotaResponse fila) -> prioridad(fila.getSituacion()))
            .thenComparing(CuotaResponse::getFechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CuotaResponse::getAlumnoApellido, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(CuotaResponse::getAlumnoNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final CuotaRepository cuotaRepository;
    private final AlumnoRepository alumnoRepository;

    public CuotaService(CuotaRepository cuotaRepository, AlumnoRepository alumnoRepository) {
        this.cuotaRepository = cuotaRepository;
        this.alumnoRepository = alumnoRepository;
    }

    /** MOGNI-46: una fila por alumno activo con su última cuota y su situación de pago. */
    @Transactional(readOnly = true)
    public List<CuotaResponse> listarCuotas() {
        LocalDate hoy = ZonaHoraria.hoy();
        Map<Long, Cuota> ultimas = cuotaRepository.buscarUltimasCuotas();
        return alumnoRepository.findByCuentaActiva(true).stream()
                .map(alumno -> mapearAResponse(alumno, ultimas.get(alumno.getId()), hoy))
                .sorted(ORDEN)
                .toList();
    }

    /**
     * MOGNI-281: la situación de pago de un alumno. Sale de la última cuota pagada (activa + programada): quien ya pagó
     * el período siguiente con otra membresía no aparece por vencer. Vacío si no tiene cuotas (solo datos viejos):
     * igual que en el listado, no se lo trata como al día.
     */
    public static Optional<EstadoCuota> estadoCuotaDe(Alumno alumno, LocalDate hoy) {
        return alumno.buscarUltimaCuotaPagada().map(cuota -> new EstadoCuota(
                SituacionPago.de(cuota, hoy),
                cuota.getFechaVencimiento(),
                ReglasCuota.diasHastaVencimiento(cuota.getFechaVencimiento(), hoy)));
    }

    static CuotaResponse mapearAResponse(Alumno alumno, Cuota cuota, LocalDate hoy) {
        // La membresía que usa hoy sale de la inscripción activa; la cuota puede ser de una programada
        Membresia membresia = alumno.buscarInscripcionActiva().map(Inscripcion::getMembresia).orElse(null);
        Inscripcion programada = alumno.buscarInscripcionProgramada().orElse(null);
        // El precio es lo que va a pagar en la próxima renovación: el de la programada si ya pagó el cambio
        Membresia aRenovar = programada != null ? programada.getMembresia() : membresia;
        SituacionPago situacion = SituacionPago.de(cuota, hoy);
        // Sin cuotas (datos viejos) pero con inscripción activa: puede renovar, el período empieza hoy
        boolean puedeRenovar = cuota != null
                ? ReglasCuota.puedeRenovar(cuota.getFechaVencimiento(), hoy)
                : alumno.buscarInscripcionActiva().isPresent();
        return new CuotaResponse(
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getDni(),
            cuota != null ? cuota.getId() : null,
            membresia != null ? membresia.getNombre() : null,
            cuota != null ? cuota.getFechaInicio() : null,
            cuota != null ? cuota.getFechaVencimiento() : null,
            aRenovar != null ? BigDecimal.valueOf(aRenovar.getPrecioActual()).setScale(2) : null,
            situacion,
            situacion == SituacionPago.EN_MORA ? ReglasCuota.diasEnMora(cuota.getFechaVencimiento(), hoy) : null,
            puedeRenovar,
            cuota != null ? cuota.getFechaVencimiento().minusDays(ReglasCuota.DIAS_VENTANA_RENOVACION) : null,
            programada != null ? programada.getMembresia().getNombre() : null,
            programada != null ? programada.getFechaInicio() : null,
            membresia != null ? membresia.getId() : null
        );
    }

    private static int prioridad(SituacionPago situacion) {
        if (situacion == null) {
            return 3;
        }
        return switch (situacion) {
            case EN_MORA -> 0;
            case VENCIDA -> 1;
            default -> 2;
        };
    }
}
