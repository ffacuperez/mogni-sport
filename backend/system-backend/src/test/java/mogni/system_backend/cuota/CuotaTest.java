package mogni.system_backend.cuota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.cuota.service.InicializacionCuotas;
import mogni.system_backend.cuota.service.VencimientoCuotasService;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.inscripcion.repository.InscripcionRepository;
import mogni.system_backend.membresia.entity.Membresia;

@ExtendWith(MockitoExtension.class)
class CuotaTest {

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private VencimientoCuotasService vencimientoCuotasService;

    @InjectMocks
    private InicializacionCuotas inicializacionCuotas;

    private Inscripcion inscripcion(LocalDate inicio) {
        Membresia membresia = new Membresia(7L, "Pro", 25000.0, 3, null);
        return new Inscripcion(5L, inicio, null, EstadoInscripcion.ACTIVA, null, membresia);
    }

    @Test
    void primeraCuota_esPendienteAlPrecioDeLaMembresiaYVenceA30Dias() {
        LocalDate inicio = LocalDate.of(2026, 10, 15);
        Inscripcion inscripcion = inscripcion(inicio);

        Cuota cuota = Cuota.primeraDe(inscripcion);

        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
        assertEquals("2026-10", cuota.getPeriodo());
        assertEquals(0, new BigDecimal("25000").compareTo(cuota.getMontoEsperado()));
        assertEquals(LocalDate.of(2026, 11, 14), cuota.getFechaVencimiento());
        assertEquals(inscripcion, cuota.getInscripcion());
    }

    @Test
    void vencimiento_marcaComoAtrasadasLasPendientesVencidas() {
        VencimientoCuotasService service = new VencimientoCuotasService(cuotaRepository);
        when(cuotaRepository.marcarAtrasadas(ZonaHoraria.hoy(), EstadoCuota.PENDIENTE, EstadoCuota.ATRASADA)).thenReturn(2);

        assertEquals(2, service.marcarCuotasAtrasadas());
        verify(cuotaRepository).marcarAtrasadas(ZonaHoraria.hoy(), EstadoCuota.PENDIENTE, EstadoCuota.ATRASADA);
    }

    @Test
    @SuppressWarnings("unchecked")
    void inicializacion_generaLaCuotaDeLasInscripcionesActivasSinCuotas() {
        Inscripcion vieja = inscripcion(LocalDate.now().minusDays(45));
        when(inscripcionRepository.findSinCuotas(EstadoInscripcion.ACTIVA)).thenReturn(List.of(vieja));

        inicializacionCuotas.run(null);

        ArgumentCaptor<List<Cuota>> captor = ArgumentCaptor.forClass(List.class);
        verify(cuotaRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(vieja, captor.getValue().get(0).getInscripcion());
        // Después de generar, las que ya vencieron pasan a ATRASADA
        verify(vencimientoCuotasService).marcarCuotasAtrasadas();
    }

    @Test
    void inicializacion_sinInscripcionesPendientesNoGeneraNada() {
        when(inscripcionRepository.findSinCuotas(EstadoInscripcion.ACTIVA)).thenReturn(List.of());

        assertEquals(0, inicializacionCuotas.generarCuotasFaltantes());
        verify(cuotaRepository).saveAll(anyList());
    }
}
