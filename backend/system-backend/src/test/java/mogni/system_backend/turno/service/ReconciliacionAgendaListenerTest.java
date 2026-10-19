package mogni.system_backend.turno.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.turno.repository.TurnoRepository;

/** El listener usa el "ahora" de la zona del gimnasio y solo mira fechas de hoy en adelante. */
@ExtendWith(MockitoExtension.class)
class ReconciliacionAgendaListenerTest {

    @Mock private TurnoRepository turnoRepository;
    @Mock private ReconciliacionTurnosService reconciliacionTurnosService;

    @InjectMocks
    private ReconciliacionAgendaListener listener;

    @Test
    void alCambiarElCupoSoloPideLasFechasDeHoyEnAdelanteYPasaElAhoraDeArgentina() {
        LocalDate hoy = ZonaHoraria.hoy();
        when(turnoRepository.findFechasConTurnosDesde(any())).thenReturn(List.of(hoy, hoy.plusDays(1)));

        listener.alCambiarElCupo(new CupoModificadoEvent(12));

        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        verify(turnoRepository).findFechasConTurnosDesde(desde.capture());
        // Tolerancia de un día por si la prueba cruza la medianoche
        assertFalse(desde.getValue().isBefore(hoy.minusDays(1)));
        assertFalse(desde.getValue().isAfter(hoy.plusDays(1)));

        ArgumentCaptor<LocalDateTime> ahora = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reconciliacionTurnosService).aplicarCupoEnFecha(eq(hoy), eq(12), ahora.capture());
        verify(reconciliacionTurnosService).aplicarCupoEnFecha(eq(hoy.plusDays(1)), eq(12), any());
        assertEquals(desde.getValue(), ahora.getValue().toLocalDate());
        assertTrue(Duration.between(ahora.getValue(), ZonaHoraria.ahora()).abs().toMinutes() < 1);
    }

    @Test
    void siFallaUnaFechaSigueConLasDemas() {
        LocalDate hoy = ZonaHoraria.hoy();
        when(turnoRepository.findFechasConTurnosDesde(any())).thenReturn(List.of(hoy, hoy.plusDays(1)));
        doThrow(new IllegalStateException("deadlock")).when(reconciliacionTurnosService)
                .aplicarCupoEnFecha(eq(hoy), eq(12), any());

        listener.alCambiarElCupo(new CupoModificadoEvent(12));

        verify(reconciliacionTurnosService).aplicarCupoEnFecha(eq(hoy.plusDays(1)), eq(12), any());
    }

    @Test
    void sinFechasConTurnosNoHaceNada() {
        when(turnoRepository.findFechasConTurnosDesde(any())).thenReturn(List.of());

        listener.alCambiarElCupo(new CupoModificadoEvent(12));

        verify(reconciliacionTurnosService, never()).aplicarCupoEnFecha(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }
}
