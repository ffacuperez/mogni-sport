package mogni.system_backend.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.auth.entity.SesionCuenta;

public interface SesionCuentaRepository extends JpaRepository<SesionCuenta, Long> {

    /** La sesión de ese token de renovación (por su hash), con la cuenta ya cargada. */
    @EntityGraph(attributePaths = "cuenta")
    Optional<SesionCuenta> findByTokenHash(String tokenHash);

    /** La sesión cuyo token anterior era ese: si alguien lo vuelve a usar, la sesión se cierra. */
    Optional<SesionCuenta> findByHashAnterior(String hashAnterior);

    /** Sesiones todavía abiertas de la cuenta (para cerrarlas todas al cambiar la contraseña). */
    List<SesionCuenta> findByCuentaIdAndCerradaEnIsNull(Long cuentaId);
}
