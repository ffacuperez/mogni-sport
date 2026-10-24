package mogni.system_backend.membresia.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.dto.MembresiaRequest;
import mogni.system_backend.membresia.dto.MembresiaResponse;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;

@Service
public class MembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final InscripcionRepository inscripcionRepository;

    public MembresiaService(MembresiaRepository membresiaRepository, InscripcionRepository inscripcionRepository) {
        this.membresiaRepository = membresiaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional
    public MembresiaResponse crearMembresia(MembresiaRequest request) {
        String nombre = request.getNombre().trim();

        if (membresiaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new RegistroInvalidoException("Ya existe una membresía con ese nombre");
        }

        Membresia membresia = new Membresia();
        aplicarDatos(membresia, request, nombre);

        return mapearAResponse(membresiaRepository.save(membresia));
    }

    public List<MembresiaResponse> listarMembresias() {
        return membresiaRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public MembresiaResponse obtenerMembresia(Long id) {
        return mapearAResponse(buscarMembresiaOLanzarError(id));
    }

    @Transactional
    public MembresiaResponse actualizarMembresia(Long id, MembresiaRequest request) {
        Membresia membresia = buscarMembresiaOLanzarError(id);
        String nombre = request.getNombre().trim();

        if (membresiaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new RegistroInvalidoException("Ya existe otra membresía con ese nombre");
        }

        aplicarDatos(membresia, request, nombre);

        return mapearAResponse(membresiaRepository.save(membresia));
    }

    @Transactional
    public void eliminarMembresia(Long id) {
        Membresia membresia = buscarMembresiaOLanzarError(id);

        if (inscripcionRepository.existsByMembresiaIdAndEstado(id, EstadoInscripcion.ACTIVA)) {
            throw new RegistroInvalidoException(
                "No se puede eliminar la membresía porque tiene alumnos con inscripciones activas");
        }
        // MP7 (20.10): alumnos que ya pagaron esta membresía para el mes siguiente
        if (inscripcionRepository.existsByMembresiaIdAndEstado(id, EstadoInscripcion.PROGRAMADA)) {
            throw new RegistroInvalidoException(
                "No se puede eliminar la membresía porque tiene alumnos que ya la pagaron para el mes siguiente");
        }

        // Las inscripciones vencidas o canceladas son historial: no se borran, así que la membresía tampoco
        if (inscripcionRepository.existsByMembresiaId(id)) {
            throw new RegistroInvalidoException(
                "No se puede eliminar la membresía porque tiene historial de inscripciones (vencidas o canceladas)");
        }

        membresiaRepository.delete(membresia);
    }

    private void aplicarDatos(Membresia membresia, MembresiaRequest request, String nombre) {
        membresia.setNombre(nombre);
        membresia.setPrecioActual(request.getPrecioActual());
        membresia.setFrecuenciaSemanal(request.getFrecuenciaSemanal());
        membresia.setDescripcion(request.getDescripcion());
    }

    private Membresia buscarMembresiaOLanzarError(Long id) {
        return membresiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una membresía con id " + id));
    }

    private MembresiaResponse mapearAResponse(Membresia membresia) {
        return new MembresiaResponse(
            membresia.getId(),
            membresia.getNombre(),
            membresia.getPrecioActual(),
            membresia.getFrecuenciaSemanal(),
            membresia.getDescripcion()
        );
    }
}
