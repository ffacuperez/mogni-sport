package mogni.system_backend.notificacion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.notificacion.dto.TipoNotificacion;

/**
 * Registro de que ya se mandó la push de una cuota en un estado ("por vencer" o "vencida"), para mandarla
 * una sola vez por estado aunque la tarea diaria la vuelva a encontrar.
 */
@Entity
@Table(name = "avisos_push_enviados", uniqueConstraints = @UniqueConstraint(columnNames = {"cuota_id", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class AvisoPushEnviado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuota_id", nullable = false)
    private Long cuotaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    public AvisoPushEnviado(Long cuotaId, TipoNotificacion tipo, LocalDateTime fechaEnvio) {
        this.cuotaId = cuotaId;
        this.tipo = tipo;
        this.fechaEnvio = fechaEnvio;
    }
}
