package mogni.system_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private List<String> autoridades(UserDetails user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void conCambioPendienteNoRecibeLosPermisosDeSuRol() {
        when(cuentaRepository.findByEmailIgnoreCase("a@x.com"))
                .thenReturn(Optional.of(new Cuenta(1L, "a@x.com", "hash", Rol.ALUMNO, true, true)));

        assertEquals(List.of(CustomUserDetailsService.AUTORIDAD_CAMBIO_PENDIENTE),
                autoridades(service.loadUserByUsername("a@x.com")));
    }

    @Test
    void sinCambioPendienteRecibeSuRol() {
        when(cuentaRepository.findByEmailIgnoreCase("p@x.com"))
                .thenReturn(Optional.of(new Cuenta(2L, "p@x.com", "hash", Rol.PROPIETARIO, false, true)));

        assertEquals(List.of("ROLE_PROPIETARIO"), autoridades(service.loadUserByUsername("p@x.com")));
    }
}
