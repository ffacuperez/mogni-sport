package mogni.system_backend.plantilla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Solo se trabaja con plantillas activas: una inactiva no se lista, no se modifica y libera su nombre.
 */
@Service
public class PlantillaService {

    /** El mismo límite que valida PlantillaRequest. */
    private static final int NOMBRE_MAX = 100;

    private final PlantillaPlanRepository plantillaRepository;
    private final CatalogoEjercicios catalogoEjercicios;

    public PlantillaService(PlantillaPlanRepository plantillaRepository, EjercicioRepository ejercicioRepository) {
        this.plantillaRepository = plantillaRepository;
        this.catalogoEjercicios = new CatalogoEjercicios(ejercicioRepository);
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

    /** Plantillas inactivas, de la modificada más recientemente a la más antigua. */
    @Transactional(readOnly = true)
    public List<PlantillaResponse> listarInactivas() {
        return plantillaRepository.findByActivaFalseOrderByFechaActualizacionDesc().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    /** Vuelve a activar una plantilla inactiva. Falla si otra plantilla activa ya usa su nombre. */
    @Transactional
    public PlantillaResponse reactivarPlantilla(Long id) {
        PlantillaPlan plantilla = plantillaRepository.findByIdAndActivaFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una plantilla inactiva con id " + id));
        if (plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue(plantilla.getNombre())) {
            throw new ConflictoException("Ya existe una plantilla activa con ese nombre");
        }
        plantilla.setActiva(true);
        plantilla.setFechaActualizacion(ZonaHoraria.ahora());
        return mapearAResponse(guardarConNombreUnico(plantilla));
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
        Map<Long, Ejercicio> catalogo = catalogoEjercicios.buscar(request.getSesiones(), ejerciciosPermitidosInactivos);

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
        SesionPlantilla sesion = new SesionPlantilla(CatalogoEjercicios.nombreDeSesion(request));
        sesion.agregarEjercicios(request.getEjercicios().stream()
                .map(e -> new EjercicioSesion(catalogo.get(e.getEjercicioId()), e.getSeries(), e.getRepeticiones(),
                        e.getRepeticionesMax(), e.getDescansoMinutos(), e.getPesoSugerido(), e.getRirObjetivo()))
                .toList());
        return sesion;
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
                sesion.getEjercicios().stream().map(EjercicioSesionResponse::desde).toList());
    }
}
