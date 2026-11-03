package mogni.system_backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import mogni.system_backend.auth.dto.CambiarPasswordRequest;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.auth.service.CuentaService;
import mogni.system_backend.exception.RegistroInvalidoException;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CuentaService cuentaService;

    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        cuenta = new Cuenta(1L, "lucia@mail.com", "hash-dni", Rol.ALUMNO, true, true, null);
        when(cuentaRepository.findByEmailIgnoreCase("lucia@mail.com")).thenReturn(Optional.of(cuenta));
    }

    private CambiarPasswordRequest pedido(String actual, String nueva) {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual(actual);
        request.setPasswordNueva(nueva);
        return request;
    }

    @Test
    void cambiaLaContraseniaYQuitaLaMarca() {
        when(passwordEncoder.matches("30111222", "hash-dni")).thenReturn(true);
        when(passwordEncoder.matches("nuevaSegura1", "hash-dni")).thenReturn(false);
        when(passwordEncoder.encode("nuevaSegura1")).thenReturn("hash-nueva");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        Cuenta actualizada = cuentaService.cambiarPassword("lucia@mail.com", pedido("30111222", "nuevaSegura1"));

        assertEquals("hash-nueva", actualizada.getPassword());
        assertFalse(actualizada.isDebeCambiarPassword());
    }

    @Test
    void rechazaContraseniaActualIncorrecta() {
        when(passwordEncoder.matches("equivocada", "hash-dni")).thenReturn(false);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> cuentaService.cambiarPassword("lucia@mail.com", pedido("equivocada", "nuevaSegura1")));

        assertEquals("La contraseña actual es incorrecta", ex.getMessage());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void rechazaQueLaNuevaSeaIgualALaActual() {
        when(passwordEncoder.matches("30111222", "hash-dni")).thenReturn(true);

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> cuentaService.cambiarPassword("lucia@mail.com", pedido("30111222", "30111222")));

        assertEquals("La contraseña nueva debe ser distinta de la actual", ex.getMessage());
        verify(cuentaRepository, never()).save(any());
    }

    // MOGNI-263: cambiar contraseña desde el perfil, con confirmación

    @Test
    void rechazaConfirmacionDistintaDeLaNueva() {
        CambiarPasswordRequest request = pedido("30111222", "nuevaSegura1");
        request.setPasswordConfirmacion("otraCosa123");

        RegistroInvalidoException ex = assertThrows(RegistroInvalidoException.class,
                () -> cuentaService.cambiarPassword("lucia@mail.com", request));

        assertEquals("La confirmación no coincide con la contraseña nueva", ex.getMessage());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void conConfirmacionIgualCambiaLaContrasenia() {
        CambiarPasswordRequest request = pedido("30111222", "nuevaSegura1");
        request.setPasswordConfirmacion("nuevaSegura1");
        when(passwordEncoder.matches("30111222", "hash-dni")).thenReturn(true);
        when(passwordEncoder.matches("nuevaSegura1", "hash-dni")).thenReturn(false);
        when(passwordEncoder.encode("nuevaSegura1")).thenReturn("hash-nueva");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("hash-nueva", cuentaService.cambiarPassword("lucia@mail.com", request).getPassword());
    }
}
