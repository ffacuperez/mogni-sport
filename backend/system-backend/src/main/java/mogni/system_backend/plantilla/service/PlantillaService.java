package mogni.system_backend.plantilla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.EjercicioSesionResponse;
import mogni.system_backend.plantilla.dto.PlantillaRequest;
import mogni.system_backend.plantilla.dto.PlantillaResponse;
import mogni.system_backend.plantilla.dto.SesionPlantillaResponse;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.EjercicioSesion;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.entity.SesionPlantilla;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;

/**
 * HU-265: alta, consulta, modificación, baja lógica y duplicado de plantillas de plan de entrenamiento.
 * Solo se trabaja con plantillas activas: una dada de baja no se lista, no se modifica y libera su nombre.
 */
@Service
public class PlantillaService {

    /** El mismo límite que valida PlantillaRequest. */
    private static final int NOMBRE_MAX = 100;

    private final PlantillaPlanRepository plantillaRepository;
    private final EjercicioRepository ejercicioRepository;

    public PlantillaService(PlantillaPlanRepository plantillaRepository, EjercicioRepository ejercicioRepository) {
        this.plantillaRepository = plantillaRepository;
        this.ejercicioRepository = ejercicioRepository;
    }

    /** Plantillas activas, de la modificada más recientemente a la más antigua. */
    @Transactional(readOnly = true)
    public List<PlantillaResponse> listarPlantillas() {
        return plantillaRepository.findByActivaTrueOrderByFechaActualizacionDesc().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantillaResponse obtenerPlantilla(Long id) {
        return mapearAResponse(buscarPlantillaOLanzarError(id));
    }

    @Transactional
    public PlantillaResponse crearPlantilla(PlantillaRequest request) {
        String nombre = request.getNombre().trim();
        if (plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue(nombre)) {
            throw new RegistroInvalidoException("Ya existe una plantilla con ese nombre");
        }
        PlantillaPlan plantilla = new PlantillaPlan();
        aplicarDatos(plantilla, request, nombre, Set.of());
        return mapearAResponse(guardarConNombreUnico(plantilla));
    }

    /** Reemplaza nombre, nivel, objetivo y todas las sesiones por las recibidas. */
    @Transactional
    public PlantillaResponse actualizarPlantilla(Long id, PlantillaRequest request) {
        PlantillaPlan plantilla = buscarPlantillaOLanzarError(id);
        String nombre = request.getNombre().trim();
        if (plantillaRepository.existsByNombreIgnoreCaseAndActivaTrueAndIdNot(nombre, id)) {
            throw new RegistroInvalidoException("Ya existe otra plantilla con ese nombre");
        }
        // Un ejercicio que se dio de baja del catálogo puede quedar en la plantilla si ya estaba
        Set<Long> ejerciciosQueYaTenia = plantilla.getSesiones().stream()
                .flatMap(sesion -> sesion.getEjercicios().stream())
                .map(ejercicioSesion -> ejercicioSesion.getEjercicio().getId())
                .collect(Collectors.toSet());
        aplicarDatos(plantilla, request, nombre, ejerciciosQueYaTenia);
        return mapearAResponse(guardarConNombreUnico(plantilla));
    }

    /** Baja lógica: la plantilla deja de listarse. No se permite si hay alumnos que la tienen asignada. */
    @Transactional
    public void eliminarPlantilla(Long id) {
        PlantillaPlan plantilla = buscarPlantillaOLanzarError(id);
        if (plantilla.tieneAsignacionesActivas()) {
            throw new ConflictoException("No se puede eliminar la plantilla porque está asignada a "
                    + plural(plantilla.getAsignacionesActivas(), "alumno", "alumnos") + " actualmente");
        }
        plantilla.setActiva(false);
        plantillaRepository.save(plantilla);
    }

    /** Crea una copia independiente ("Nombre (copia)") con sus propias sesiones y ejercicios. */
    @Transactional
    public PlantillaResponse duplicarPlantilla(Long id) {
        PlantillaPlan original = buscarPlantillaOLanzarError(id);
        PlantillaPlan copia = original.duplicar(nombreParaCopia(original.getNombre()), ZonaHoraria.ahora());
        return mapearAResponse(guardarConNombreUnico(copia));
    }

    // --- Auxiliares ---

    /** Guarda ya mismo: si otra operación simultánea tomó el nombre, responde como un nombre repetido y no con un 500. */
    private PlantillaPlan guardarConNombreUnico(PlantillaPlan plantilla) {
        try {
            PlantillaPlan guardada = plantillaRepository.save(plantilla);
            plantillaRepository.flush();
            return guardada;
        } catch (DataIntegrityViolationException e) {
            throw new RegistroInvalidoException("Ya existe una plantilla con ese nombre");
        }
    }

    private void aplicarDatos(PlantillaPlan plantilla, PlantillaRequest request, String nombre,
                              Set<Long> ejerciciosPermitidosInactivos) {
        if (request.getSesiones() == null || request.getSesiones().isEmpty()) {
            throw new RegistroInvalidoException("La plantilla tiene que tener al menos una sesión");
        }
        Map<Long, Ejercicio> catalogo = buscarEjercicios(request.getSesiones(), ejerciciosPermitidosInactivos);

        plantilla.setNombre(nombre);
        plantilla.setNivel(request.getNivel());
        plantilla.setObjetivo(request.getObjetivo());
        plantilla.setFechaActualizacion(ZonaHoraria.ahora());
        plantilla.reemplazarSesiones(request.getSesiones().stream()
                .map(sesionRequest -> crearSesion(sesionRequest, catalogo))
                .toList());
    }

    private SesionPlantilla crearSesion(SesionRequest request, Map<Long, Ejercicio> catalogo) {
        if (request.getEjercicios() == null || request.getEjercicios().isEmpty()) {
            throw new RegistroInvalidoException("Cada sesión tiene que tener al menos un ejercicio");
        }
        String nombre = request.getNombre() == null || request.getNombre().isBlank() ? null : request.getNombre().trim();
        SesionPlantilla sesion = new SesionPlantilla(nombre);
        sesion.agregarEjercicios(request.getEjercicios().stream()
                .map(e -> new EjercicioSesion(catalogo.get(e.getEjercicioId()), e.getSeries(), e.getRepeticiones(),
                        e.getPesoSugerido(), e.getRirObjetivo()))
                .toList());
        return sesion;
    }

    /** Trae de una vez todos los ejercicios pedidos y verifica que existan y estén activos. */
    private Map<Long, Ejercicio> buscarEjercicios(List<SesionRequest> sesiones, Set<Long> permitidosInactivos) {
        Set<Long> ids = sesiones.stream()
                .filter(sesion -> sesion.getEjercicios() != null)
                .flatMap(sesion -> sesion.getEjercicios().stream())
                .map(EjercicioSesionRequest::getEjercicioId)
                .collect(Collectors.toSet());
        Map<Long, Ejercicio> encontrados = ejercicioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ejercicio::getId, Function.identity()));

        for (Long id : ids) {
            Ejercicio ejercicio = encontrados.get(id);
            if (ejercicio == null) {
                throw new RegistroInvalidoException("No existe un ejercicio con id " + id + " en el catálogo");
            }
            if (!ejercicio.isActivo() && !permitidosInactivos.contains(id)) {
                throw new RegistroInvalidoException("El ejercicio \"" + ejercicio.getNombre() + "\" está dado de baja");
            }
        }
        return encontrados;
    }

    /** "Fuerza base" -> "Fuerza base (copia)", y si ya existe, "(copia 2)", "(copia 3)"... */
    private String nombreParaCopia(String nombre) {
        String base = nombre.replaceFirst("\\s*\\(copia(?: \\d+)?\\)$", "");
        String candidato = conSufijo(base, "(copia)");
        for (int i = 2; plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue(candidato); i++) {
            candidato = conSufijo(base, "(copia " + i + ")");
        }
        return candidato;
    }

    /** Recorta la base si hace falta: la copia tiene que respetar el largo máximo para poder editarse después. */
    private static String conSufijo(String base, String sufijo) {
        int lugar = NOMBRE_MAX - sufijo.length() - 1;
        String recortada = base.length() > lugar ? base.substring(0, lugar) : base;
        return (recortada.stripTrailing() + " " + sufijo).strip();
    }

    private PlantillaPlan buscarPlantillaOLanzarError(Long id) {
        return plantillaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una plantilla con id " + id));
    }

    private static String plural(int n, String singular, String varios) {
        return n + " " + (n == 1 ? singular : varios);
    }

    private PlantillaResponse mapearAResponse(PlantillaPlan plantilla) {
        return new PlantillaResponse(
                plantilla.getId(),
                plantilla.getNombre(),
                plantilla.getNivel(),
                plantilla.getObjetivo(),
                plantilla.getAsignacionesActivas(),
                plantilla.getFechaActualizacion(),
                plantilla.getSesiones().stream().map(this::mapearSesion).toList());
    }

    private SesionPlantillaResponse mapearSesion(SesionPlantilla sesion) {
        return new SesionPlantillaResponse(
                sesion.getId(),
                sesion.getNumero(),
                sesion.getNombre(),
                sesion.getEjercicios().stream().map(this::mapearEjercicio).toList());
    }

    private EjercicioSesionResponse mapearEjercicio(EjercicioSesion ejercicioSesion) {
        Ejercicio ejercicio = ejercicioSesion.getEjercicio();
        return new EjercicioSesionResponse(
                ejercicioSesion.getId(),
                ejercicio.getId(),
                ejercicio.getNombre(),
                ejercicio.getGrupoMuscular(),
                ejercicio.isActivo(),
                ejercicioSesion.getSeries(),
                ejercicioSesion.getRepeticiones(),
                ejercicioSesion.getPesoSugerido(),
                ejercicioSesion.getRirObjetivo());
    }
}
