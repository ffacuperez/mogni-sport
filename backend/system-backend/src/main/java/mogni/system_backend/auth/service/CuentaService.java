package mogni.system_backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.auth.dto.CambiarPasswordRequest;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    public CuentaService(CuentaRepository cuentaRepository, PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * HU-248: cambia la contraseña de la cuenta logueada y marca que ya no debe cambiarla.
     * El largo mínimo lo valida el DTO; acá se valida la actual y que la nueva sea distinta.
     */
    @Transactional
    public Cuenta cambiarPassword(String email, CambiarPasswordRequest request) {
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));

        if (!passwordEncoder.matches(request.getPasswordActual(), cuenta.getPassword())) {
            throw new RegistroInvalidoException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(request.getPasswordNueva(), cuenta.getPassword())) {
            throw new RegistroInvalidoException("La contraseña nueva debe ser distinta de la actual");
        }

        cuenta.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        cuenta.setDebeCambiarPassword(false);
        return cuentaRepository.save(cuenta);
    }
}
