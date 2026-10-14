package mogni.system_backend.notificacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.notificacion.dto.TipoNotificacion;
import mogni.system_backend.notificacion.entity.AvisoPushEnviado;

public interface AvisoPushEnviadoRepository extends JpaRepository<AvisoPushEnviado, Long> {

    boolean existsByCuotaIdAndTipo(Long cuotaId, TipoNotificacion tipo);
}
