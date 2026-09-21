package mogni.system_backend.membresia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
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
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private MembresiaService membresiaService;

    private MembresiaRequest request;

    @BeforeEach
    void setUp() {
        request = new MembresiaRequest();
        request.setNombre("  Pro ");
        request.setDiasPorSemana(5);
        request.setPrecio(new BigDecimal("40000"));
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
        assertEquals(5, response.getDiasPorSemana());
        assertEquals(30, response.getDuracionDias());
    }

    @Test
    void actualizarMembresia_sinDuracionConservaLaActual() {
        Membresia existente = new Membresia(2L, "Medio", "3 clases", 15, null, new BigDecimal("30000"));
        when(membresiaRepository.findById(2L)).thenReturn(Optional.of(existente));
        when(membresiaRepository.existsByNombreIgnoreCaseAndIdNot("Pro", 2L)).thenReturn(false);
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        MembresiaResponse response = membresiaService.actualizarMembresia(2L, request);

        assertEquals(15, response.getDuracionDias());
        assertEquals(5, response.getDiasPorSemana());
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
    void eliminarMembresia_rechazaSiTieneAlumnosAsociados() {
        Membresia membresia = new Membresia();
        when(membresiaRepository.findById(3L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.existsByMembresiaId(3L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> membresiaService.eliminarMembresia(3L));
        verify(membresiaRepository, never()).delete(any());
    }

    @Test
    void eliminarMembresia_eliminaSiNoTieneAlumnos() {
        Membresia membresia = new Membresia();
        when(membresiaRepository.findById(3L)).thenReturn(Optional.of(membresia));
        when(alumnoRepository.existsByMembresiaId(3L)).thenReturn(false);

        membresiaService.eliminarMembresia(3L);

        verify(membresiaRepository).delete(membresia);
    }
}
