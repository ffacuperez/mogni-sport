package mogni.system_backend.cuota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.cuota.dto.CuotaResponse;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.cuota.service.CuotaService;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;

/**
 * HU-46: la pantalla de cuotas solo debe traer alumnos con inscripción ACTIVA (los de inscripción
 * CANCELADA no aparecen), y respetar los filtros de estado/alumno al pedirlos. El cálculo de
 * "Próxima a vencer" es responsabilidad del frontend (cuotasService.js); acá solo se prueba
 * lo que hace el backend: a quién trae y cómo mapea los datos.
 */
@ExtendWith(MockitoExtension.class)
class CuotaServiceTest {

    @Mock
    private CuotaRepository cuotaRepository;

    @InjectMocks
    private CuotaService cuotaService;

    private Cuota cuotaDe(Long id, String nombreAlumno) {
        Alumno alumno = new Alumno();
        alumno.setId(1L);
        alumno.setNombre(nombreAlumno);
        alumno.setApellido("Perez");
        alumno.setDni("12345678");
        Membresia membresia = new Membresia(7L, "Pro", 25000.0, 3, null);
        Inscripcion inscripcion = new Inscripcion(5L, LocalDate.now().minusDays(10), null,
                EstadoInscripcion.ACTIVA, alumno, membresia);
        Cuota cuota = Cuota.primeraDe(inscripcion);
        cuota.setId(id);
        return cuota;
    }

    @Test
    void listarCuotas_siempre_pide_inscripciones_activas_sin_importar_los_filtros() {
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, null)).thenReturn(List.of());

        cuotaService.listarCuotas(null, null);

        // Las cuotas de alumnos con inscripción CANCELADA nunca llegan al repositorio (CA4 de HU-46).
        verify(cuotaRepository).buscar(EstadoInscripcion.ACTIVA, null, null);
    }

    @Test
    void listarCuotas_propaga_los_filtros_de_estado_y_alumno() {
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, EstadoCuota.ATRASADA, 9L)).thenReturn(List.of());

        cuotaService.listarCuotas(EstadoCuota.ATRASADA, 9L);

        verify(cuotaRepository).buscar(EstadoInscripcion.ACTIVA, EstadoCuota.ATRASADA, 9L);
    }

    @Test
    void listarCuotas_mapea_los_datos_del_alumno_y_de_la_cuota() {
        Cuota cuota = cuotaDe(100L, "Ana");
        when(cuotaRepository.buscar(EstadoInscripcion.ACTIVA, null, null)).thenReturn(List.of(cuota));

        List<CuotaResponse> resultado = cuotaService.listarCuotas(null, null);

        assertEquals(1, resultado.size());
        CuotaResponse response = resultado.get(0);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getAlumnoId());
        assertEquals("Ana", response.getAlumnoNombre());
        assertEquals("Perez", response.getAlumnoApellido());
        assertEquals("12345678", response.getAlumnoDni());
        assertEquals("Pro", response.getMembresiaNombre());
        assertEquals(EstadoCuota.PENDIENTE, response.getEstado());
    }
}
