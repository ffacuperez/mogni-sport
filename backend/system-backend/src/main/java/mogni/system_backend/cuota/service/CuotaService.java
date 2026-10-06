package mogni.system_backend.cuota.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;

@Service
public class CuotaService {

    private final CuotaRepository cuotaRepository;

    public CuotaService(CuotaRepository cuotaRepository) {
        this.cuotaRepository = cuotaRepository;
    }

    /** Cuotas de alumnos activos con inscripción ACTIVA, filtradas opcionalmente por estado y/o alumno. */
    @Transactional(readOnly = true)
    public List<CuotaResponse> listarCuotas(EstadoCuota estado, Long alumnoId) {
        return cuotaRepository.buscar(EstadoInscripcion.ACTIVA, estado, alumnoId)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    private CuotaResponse mapearAResponse(Cuota cuota) {
        Alumno alumno = cuota.getInscripcion().getAlumno();
        return new CuotaResponse(
            cuota.getId(),
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getDni(),
            cuota.getInscripcion().getMembresia().getNombre(),
            cuota.getPeriodo(),
            cuota.getMontoEsperado(),
            cuota.getFechaVencimiento(),
            cuota.getEstado()
        );
    }
}
