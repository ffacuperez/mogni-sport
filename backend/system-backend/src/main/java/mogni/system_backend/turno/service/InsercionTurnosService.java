package mogni.system_backend.turno.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.turno.entity.Turno;
import mogni.system_backend.turno.repository.TurnoRepository;

/**
 * Guarda todos los turnos de una fecha en una transacción propia (REQUIRES_NEW). Está en un bean aparte para
 * que, si otra consulta ya los generó y falla la restricción única, el error se pueda atrapar desde
 * {@link GeneracionTurnosService} sin dejar marcada para rollback la transacción de quien llama.
 */
@Service
public class InsercionTurnosService {

    private final TurnoRepository turnoRepository;

    public InsercionTurnosService(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Turno> insertar(List<Turno> turnos) {
        // Flush acá adentro: si hay duplicados, la excepción sale de este método y no al confirmar más tarde
        return turnoRepository.saveAllAndFlush(turnos);
    }
}
