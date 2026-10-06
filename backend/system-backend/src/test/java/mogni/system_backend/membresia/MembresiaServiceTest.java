package mogni.system_backend.membresia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.dto.MembresiaRequest;
import mogni.system_backend.membresia.dto.MembresiaResponse;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.membresia.service.MembresiaService;

@ExtendWith(MockitoExtension.class)
class MembresiaServiceTest {

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private InscripcionRepository inscripcionRepository;

    @InjectMocks
    private MembresiaService membresiaService;

    private MembresiaRequest request;

    @BeforeEach
    void setUp() {
        request = new MembresiaRequest();
        request.setNombre("  Pro ");
        request.setFrecuenciaSemanal(5);
        request.setPrecioActual(40000.0);
    }

    @Test
    void crearMembresia_guardaConNombreSinEspacios() {
        when(membresiaRepository.existsByNombreIgnoreCase("Pro")).thenReturn(false);
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> {
            Membresia m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        MembresiaResponse response = membresiaService.crearMembresia(request);

        assertEquals(1L, response.getId());
        assertEquals("Pro", response.getNombre());
        assertEquals(5, response.getFrecuenciaSemanal());
        assertEquals(40000.0, response.getPrecioActual());
    }

    @Test
    void actualizarMembresia_actualizaNombreFrecuenciaYPrecio() {
        Membresia existente = new Membresia(2L, "Medio", 30000.0, 3, "3 clases");
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(existente));
        when(membresiaRepository.existsByNombreIgnoreCaseAndIdNot("Pro", 2L)).thenReturn(false);
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        MembresiaResponse response = membresiaService.actualizarMembresia(2L, request);

        assertEquals("Pro", response.getNombre());
        assertEquals(5, response.getFrecuenciaSemanal());
        assertEquals(40000.0, response.getPrecioActual());
    }

    @Test
    void crearMembresia_rechazaNombreDuplicado() {
        when(membresiaRepository.existsByNombreIgnoreCase("Pro")).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> membresiaService.crearMembresia(request));
        verify(membresiaRepository, never()).save(any());
    }

    @Test
    void actualizarMembresia_lanzaErrorSiNoExiste() {
        when(membresiaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> membresiaService.actualizarMembresia(9L, request));
    }

    @Test
    void actualizarMembresia_rechazaNombreDeOtraMembresia() {
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(new Membresia()));
        when(membresiaRepository.existsByNombreIgnoreCaseAndIdNot("Pro", 2L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> membresiaService.actualizarMembresia(2L, request));
    }

    @Test
    void eliminarMembresia_rechazaSiTieneInscripcionesActivas() {
        Membresia membresia = new Membresia();
        when(membresiaRepository.findById(3L)).thenReturn(Optional.of(membresia));
        when(inscripcionRepository.existsByMembresiaIdAndEstado(3L, EstadoInscripcion.ACTIVA)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> membresiaService.eliminarMembresia(3L));
        verify(membresiaRepository, never()).delete(any());
    }

    @Test
    void eliminarMembresia_rechazaSiTieneHistorialDeInscripciones() {
        // Solo inscripciones CANCELADAS: el historial no se pierde, así que no se elimina
        Membresia membresia = new Membresia();
        when(membresiaRepository.findById(3L)).thenReturn(Optional.of(membresia));
        when(inscripcionRepository.existsByMembresiaIdAndEstado(3L, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.existsByMembresiaId(3L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> membresiaService.eliminarMembresia(3L));
        verify(membresiaRepository, never()).delete(any());
    }

    @Test
    void eliminarMembresia_eliminaSiNuncaTuvoInscripciones() {
        Membresia membresia = new Membresia();
        when(membresiaRepository.findById(3L)).thenReturn(Optional.of(membresia));
        when(inscripcionRepository.existsByMembresiaIdAndEstado(3L, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.existsByMembresiaId(3L)).thenReturn(false);

        membresiaService.eliminarMembresia(3L);

        verify(membresiaRepository).delete(membresia);
    }
}
