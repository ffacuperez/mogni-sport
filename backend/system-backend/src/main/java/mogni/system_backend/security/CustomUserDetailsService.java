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

    private final CuentaRepository cuentaRepository;

    public CustomUserDetailsService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No existe una cuenta con ese email"));

        return User.builder()
                .username(cuenta.getEmail())
                .password(cuenta.getPassword())
                .authorities("ROLE_" + cuenta.getRol().name())
                .disabled(!cuenta.isActiva())
                .build();
    }
}