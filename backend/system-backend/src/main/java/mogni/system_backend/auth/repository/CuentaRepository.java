package mogni.system_backend.auth.repository;

import mogni.system_backend.auth.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<Cuenta> findByEmailIgnoreCase(String email);
}