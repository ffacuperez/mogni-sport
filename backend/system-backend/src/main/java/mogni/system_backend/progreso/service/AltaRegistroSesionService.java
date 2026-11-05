package mogni.system_backend.progreso.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.entity.SesionPlan;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.progreso.entity.RegistroEjercicio;
import mogni.system_backend.progreso.entity.RegistroSesion;
import mogni.system_backend.progreso.repository.RegistroSesionRepository;

/**
 * HU-269: crea el registro de una sesión en una transacción propia (REQUIRES_NEW). Está en un bean aparte para que,
 * si otro pedido ya la inició y falla la restricción única, el error se pueda atrapar desde {@link MiProgresoService}
 * sin dejar marcada para rollback la transacción de quien llama.
 */
@Service
public class AltaRegistroSesionService {

    private final PlanAsignadoRepository planRepository;
    private final RegistroSesionRepository registroRepository;

    public AltaRegistroSesionService(PlanAsignadoRepository planRepository, RegistroSesionRepository registroRepository) {
        this.planRepository = planRepository;
        this.registroRepository = registroRepository;
    }

    /** La sesión empieza con una copia de cada ejercicio del plan, todavía sin series. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void crear(Long planId, int semana, int numeroSesion, LocalDate fecha) {
        PlanAsignado plan = planRepository.findById(planId).orElseThrow();
        SesionPlan sesionPlan = plan.getSesiones().stream()
                .filter(sesion -> sesion.getNumero() == numeroSesion)
                .findFirst()
                .orElseThrow();
        RegistroSesion registro = new RegistroSesion(plan, semana, numeroSesion, fecha);
        registro.agregarEjercicios(sesionPlan.getEjercicios().stream().map(RegistroEjercicio::new).toList());
        // Flush acá adentro: si ya existe, la excepción sale de este método y no al confirmar más tarde
        registroRepository.saveAndFlush(registro);
    }
}
