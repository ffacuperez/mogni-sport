package mogni.system_backend.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.auth.entity.TokenRecuperacionPassword;

public interface TokenRecuperacionPasswordRepository extends JpaRepository<TokenRecuperacionPassword, Long> {

    /** El pedido de ese token (por su hash), con la cuenta ya cargada. */
    @EntityGraph(attributePaths = "cuenta")
    Optional<TokenRecuperacionPassword> findByTokenHash(String tokenHash);

    /** Pedidos todavía sin usar de la cuenta (para invalidarlos al pedir uno nuevo o al cambiar la contraseña). */
    List<TokenRecuperacionPassword> findByCuentaIdAndUsadoEnIsNull(Long cuentaId);

    /** Cuántos pedidos hizo la cuenta desde ese momento (límite de pedidos seguidos). */
    long countByCuentaIdAndCreadoEnAfter(Long cuentaId, LocalDateTime desde);
}
