package mogni.system_backend.turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.turno.dto.CupoMaximoRequest;
import mogni.system_backend.turno.entity.ConfiguracionGimnasio;
import mogni.system_backend.turno.repository.ConfiguracionGimnasioRepository;
import mogni.system_backend.turno.service.ConfiguracionGimnasioService;

@ExtendWith(MockitoExtension.class)
class ConfiguracionGimnasioServiceTest {

    @Mock
    private ConfiguracionGimnasioRepository configuracionGimnasioRepository;

    @InjectMocks
    private ConfiguracionGimnasioService configuracionGimnasioService;

    // Prueba 14
    @Test
    void cambiarCupoMaximo_valorValidoSeGuarda() {
        ConfiguracionGimnasio configuracion = new ConfiguracionGimnasio(1L, 5);
        when(configuracionGimnasioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracion));
        when(configuracionGimnasioRepository.save(any(ConfiguracionGimnasio.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(12, configuracionGimnasioService.cambiarCupoMaximo(12).getCupoMaximo());
        assertEquals(12, configuracion.getCupoMaximo());
    }

    // Pruebas 15 y 16
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void cambiarCupoMaximo_ceroONegativoFalla(int cupo) {
        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> configuracionGimnasioService.cambiarCupoMaximo(cupo));

        assertEquals("El cupo máximo tiene que ser un número entero mayor a 0.", ex.getMessage());
        verify(configuracionGimnasioRepository, never()).save(any());
    }

    @Test
    void obtenerConfiguracion_devuelveElCupoVigente() {
        when(configuracionGimnasioRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(new ConfiguracionGimnasio(1L, 5)));

        assertEquals(5, configuracionGimnasioService.obtenerConfiguracion().getCupoMaximo());
    }

    // El request también rechaza 0, negativos y vacío antes de llegar al servicio
    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void cupoMaximoRequest_ceroONegativoEsInvalido(int cupo) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            CupoMaximoRequest request = new CupoMaximoRequest();
            request.setCupoMaximo(cupo);
            assertFalse(validator.validate(request).isEmpty());

            request.setCupoMaximo(null);
            assertFalse(validator.validate(request).isEmpty());

            request.setCupoMaximo(1);
            assertTrue(validator.validate(request).isEmpty());
        }
    }
}
