package mogni.system_backend.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.repository.CuentaRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    public static final String AUTORIDAD_CAMBIO_PENDIENTE = "CAMBIO_PASSWORD_PENDIENTE";

    private final CuentaRepository cuentaRepository;

    public CustomUserDetailsService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No existe una cuenta con ese email"));

        // HU-248: mientras deba cambiar la contraseña no recibe los permisos de su rol,
        // así solo puede usar /auth/cambiar-password (que solo exige estar autenticado)
        String autoridad = cuenta.isDebeCambiarPassword()
                ? AUTORIDAD_CAMBIO_PENDIENTE
                : "ROLE_" + cuenta.getRol().name();

        return User.builder()
                .username(cuenta.getEmail())
                .password(cuenta.getPassword())
                .authorities(autoridad)
                .disabled(!cuenta.isActiva())
                .build();
    }
}