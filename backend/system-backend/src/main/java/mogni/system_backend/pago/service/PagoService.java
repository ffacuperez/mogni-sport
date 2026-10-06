package mogni.system_backend.pago.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.pago.dto.PagoResponse;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;

    public PagoService(PagoRepository pagoRepository, CuotaRepository cuotaRepository) {
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    /**
     * Registra el pago completo de una cuota PENDIENTE o ATRASADA y la deja PAGADA.
     * La fecha de pago es siempre el día actual; no se admiten pagos parciales ni excedentes.
     */
    @Transactional
    public PagoResponse registrarPago(RegistrarPagoRequest request) {
        Cuota cuota = cuotaRepository.findByIdParaActualizar(request.getCuotaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuota seleccionada no existe"));

        if (cuota.getEstado() == EstadoCuota.PAGADA) {
            log.warn("Pago rechazado: la cuota {} ya está PAGADA", cuota.getId());
            throw new RegistroInvalidoException("La cuota seleccionada ya está pagada");
        }

        if (cuota.getInscripcion().getEstado() != EstadoInscripcion.ACTIVA) {
            log.warn("Pago rechazado: la cuota {} pertenece a la inscripción {} en estado {}",
                    cuota.getId(), cuota.getInscripcion().getId(), cuota.getInscripcion().getEstado());
            throw new RegistroInvalidoException("La cuota corresponde a una inscripción cancelada");
        }

        Alumno alumno = cuota.getInscripcion().getAlumno();
        if (!alumno.getCuenta().isActiva()) {
            log.warn("Pago rechazado: el alumno {} de la cuota {} está inactivo", alumno.getId(), cuota.getId());
            throw new RegistroInvalidoException("No se pueden registrar pagos a un alumno inactivo");
        }

        // compareTo y no equals: 25000 y 25000.00 son el mismo monto
        if (request.getMonto().compareTo(cuota.getMontoEsperado()) != 0) {
            log.warn("Pago rechazado: monto {} distinto al esperado {} en la cuota {}",
                    request.getMonto(), cuota.getMontoEsperado(), cuota.getId());
            throw new RegistroInvalidoException("El monto debe coincidir con el monto de la cuota");
        }

        Pago pago = new Pago();
        pago.setCuota(cuota);
        pago.setMonto(cuota.getMontoEsperado());
        pago.setMedioDePago(request.getMedioDePago());
        pago.setFechaPago(ZonaHoraria.hoy());
        cuota.setEstado(EstadoCuota.PAGADA);

        Pago guardado = pagoRepository.save(pago);
        log.info("Pago {} registrado: cuota {} ({}) del alumno {} por {} en {}", guardado.getId(), cuota.getId(),
                cuota.getPeriodo(), alumno.getId(), guardado.getMonto(), guardado.getMedioDePago());

        return new PagoResponse(guardado.getId(), cuota.getId(), cuota.getPeriodo(), guardado.getMonto(),
                guardado.getMedioDePago(), guardado.getFechaPago(), cuota.getEstado());
    }
}
