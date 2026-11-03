package mogni.system_backend.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recuperar contraseña: pedido de restablecimiento enviado por mail. Se guarda solo el hash SHA-256 del token (el
 * token en claro viaja únicamente en el link del mail), así una copia de la base no sirve para cambiar contraseñas.
 * Es de un solo uso y vence a los pocos minutos.
 */
@Entity
@Table(name = "tokens_recuperacion_password")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacionPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    /** SHA-256 del token, en hexadecimal. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime venceEn;

    /** Cuándo se usó o se invalidó (por un pedido nuevo o un cambio de contraseña). Null = todavía sirve. */
    private LocalDateTime usadoEn;

    public TokenRecuperacionPassword(Cuenta cuenta, String tokenHash, LocalDateTime creadoEn, LocalDateTime venceEn) {
        this.cuenta = cuenta;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.venceEn = venceEn;
    }

    /** Se puede usar: no se usó ni se invalidó y todavía no venció. */
    public boolean esValido(LocalDateTime ahora) {
        return usadoEn == null && ahora.isBefore(venceEn);
    }

    public void marcarUsado(LocalDateTime ahora) {
        this.usadoEn = ahora;
    }
}
