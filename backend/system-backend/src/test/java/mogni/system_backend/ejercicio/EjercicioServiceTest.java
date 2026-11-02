package mogni.system_backend.ejercicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.ejercicio.service.EjercicioService;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;

/** HU-265: un ejercicio que se usa en alguna plantilla no se puede eliminar (sí darlo de baja). */
@ExtendWith(MockitoExtension.class)
class EjercicioServiceTest {

    @Mock
    private EjercicioRepository ejercicioRepository;

    @Mock
    private PlantillaPlanRepository plantillaRepository;

    @Mock
    private PlanAsignadoRepository planRepository;

    @InjectMocks
    private EjercicioService ejercicioService;

    private final Ejercicio sentadilla = new Ejercicio(3L, "Sentadilla", "Piernas", null, true);

    @Test
    void eliminar_un_ejercicio_que_se_usa_en_una_plantilla_falla() {
        when(ejercicioRepository.findById(3L)).thenReturn(Optional.of(sentadilla));
        when(plantillaRepository.existeEjercicioEnAlgunaPlantilla(3L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> ejercicioService.eliminarEjercicio(3L));
        verify(ejercicioRepository, never()).delete(any());
    }

    @Test
    void eliminar_un_ejercicio_que_se_usa_en_el_plan_de_un_alumno_falla() {
        when(ejercicioRepository.findById(3L)).thenReturn(Optional.of(sentadilla));
        when(planRepository.existeEjercicioEnAlgunPlan(3L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class, () -> ejercicioService.eliminarEjercicio(3L));
        verify(ejercicioRepository, never()).delete(any());
    }

    @Test
    void eliminar_un_ejercicio_que_no_esta_en_ninguna_plantilla_lo_borra() {
        when(ejercicioRepository.findById(3L)).thenReturn(Optional.of(sentadilla));
        when(plantillaRepository.existeEjercicioEnAlgunaPlantilla(3L)).thenReturn(false);

        ejercicioService.eliminarEjercicio(3L);

        verify(ejercicioRepository).delete(sentadilla);
    }
}
