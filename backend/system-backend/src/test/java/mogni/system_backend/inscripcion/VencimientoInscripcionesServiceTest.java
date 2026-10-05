package mogni.system_backend.inscripcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.inscripcion.service.VencimientoInscripcionesService;

@ExtendWith(MockitoExtension.class)
class VencimientoInscripcionesServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    @InjectMocks
    private VencimientoInscripcionesService service;

    @Test
    void marcaComoVencidasLasActivasConFechaFinAnteriorAHoy() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        when(inscripcionRepository.marcarVencidas(any(), any(), any())).thenReturn(2);

        int vencidas = service.marcarInscripcionesVencidas();

        assertEquals(2, vencidas);
        verify(inscripcionRepository).marcarVencidas(hoy, EstadoInscripcion.ACTIVA, EstadoInscripcion.VENCIDA);
    }
}
