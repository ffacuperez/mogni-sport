package mogni.system_backend.repository;

import mogni.system_backend.entidades.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    boolean existsByEmail(String email);
    Optional<Cuenta> findByEmail(String email);
}