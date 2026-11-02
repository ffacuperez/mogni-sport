package mogni.system_backend.plantilla.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.SesionRequest;

/** Busca los ejercicios que piden las sesiones de una plantilla o de un plan asignado (HU-265 y HU-266). */
public class CatalogoEjercicios {

    private final EjercicioRepository ejercicioRepository;

    public CatalogoEjercicios(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    /**
     * Trae de una vez todos los ejercicios pedidos y verifica que existan y estén activos. Los de
     * permitidosInactivos pueden estar dados de baja (ya estaban en lo que se está modificando).
     */
    public Map<Long, Ejercicio> buscar(List<SesionRequest> sesiones, Set<Long> permitidosInactivos) {
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

    /** Nombre opcional de una sesión: sin espacios de más, o null si vino vacío. */
    public static String nombreDeSesion(SesionRequest request) {
        return request.getNombre() == null || request.getNombre().isBlank() ? null : request.getNombre().trim();
    }
}
