package mogni.system_backend.ejercicio.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.ejercicio.dto.EjercicioRequest;
import mogni.system_backend.ejercicio.dto.EjercicioResponse;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;

@Service
public class EjercicioService {

    private final EjercicioRepository ejercicioRepository;
    private final PlantillaPlanRepository plantillaRepository;
    private final PlanAsignadoRepository planRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository, PlantillaPlanRepository plantillaRepository,
                            PlanAsignadoRepository planRepository) {
        this.ejercicioRepository = ejercicioRepository;
        this.plantillaRepository = plantillaRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public EjercicioResponse crearEjercicio(EjercicioRequest request) {
        String nombre = request.getNombre().trim();

        if (ejercicioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new RegistroInvalidoException("Ya existe un ejercicio con ese nombre");
        }

        Ejercicio ejercicio = new Ejercicio();
        aplicarDatos(ejercicio, request, nombre);

        return mapearAResponse(ejercicioRepository.save(ejercicio));
    }

    public List<EjercicioResponse> listarEjercicios() {
        return ejercicioRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public EjercicioResponse obtenerEjercicio(Long id) {
        return mapearAResponse(buscarEjercicioOLanzarError(id));
    }

    @Transactional
    public EjercicioResponse actualizarEjercicio(Long id, EjercicioRequest request) {
        Ejercicio ejercicio = buscarEjercicioOLanzarError(id);
        String nombre = request.getNombre().trim();

        if (ejercicioRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new RegistroInvalidoException("Ya existe otro ejercicio con ese nombre");
        }

        aplicarDatos(ejercicio, request, nombre);

        return mapearAResponse(ejercicioRepository.save(ejercicio));
    }

    @Transactional
    public EjercicioResponse toggleEstadoEjercicio(Long id) {
        Ejercicio ejercicio = buscarEjercicioOLanzarError(id);
        ejercicio.setActivo(!ejercicio.isActivo());
        return mapearAResponse(ejercicioRepository.save(ejercicio));
    }

    @Transactional
    public void eliminarEjercicio(Long id) {
        Ejercicio ejercicio = buscarEjercicioOLanzarError(id);
        // HU-265 y HU-266: borrarlo dejaría sesiones de plantillas o de planes de alumnos sin su ejercicio;
        // se puede dar de baja en su lugar
        if (plantillaRepository.existeEjercicioEnAlgunaPlantilla(id)) {
            throw new RegistroInvalidoException(
                "No se puede eliminar el ejercicio porque se usa en plantillas de entrenamiento. Podés darlo de baja");
        }
        if (planRepository.existeEjercicioEnAlgunPlan(id)) {
            throw new RegistroInvalidoException(
                "No se puede eliminar el ejercicio porque se usa en planes de entrenamiento de alumnos. Podés darlo de baja");
        }
        ejercicioRepository.delete(ejercicio);
    }

    private void aplicarDatos(Ejercicio ejercicio, EjercicioRequest request, String nombre) {
        ejercicio.setNombre(nombre);
        ejercicio.setGrupoMuscular(request.getGrupoMuscular());
        if (request.getActivo() != null) {
            ejercicio.setActivo(request.getActivo());
        }
    }

    private Ejercicio buscarEjercicioOLanzarError(Long id) {
        return ejercicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un ejercicio con id " + id));
    }

    private EjercicioResponse mapearAResponse(Ejercicio ejercicio) {
        return new EjercicioResponse(
            ejercicio.getId(),
            ejercicio.getNombre(),
            ejercicio.getGrupoMuscular(),
            ejercicio.isActivo()
        );
    }
}