package mogni.system_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.entidades.Cuenta;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    boolean existsByEmail(String email);
}