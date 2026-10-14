package mogni.system_backend.notificacion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.notificacion.entity.SuscripcionPush;

public interface SuscripcionPushRepository extends JpaRepository<SuscripcionPush, Long> {

    Optional<SuscripcionPush> findByEndpoint(String endpoint);

    List<SuscripcionPush> findByCuentaId(Long cuentaId);

    List<SuscripcionPush> findByCuentaEmailIgnoreCase(String email);

    @Transactional
    void deleteByEndpoint(String endpoint);

    @Transactional
    void deleteByEndpointAndCuentaEmailIgnoreCase(String endpoint, String email);
}
