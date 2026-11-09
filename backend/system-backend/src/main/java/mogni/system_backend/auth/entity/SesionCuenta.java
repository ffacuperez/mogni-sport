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

/**
 * Sesión iniciada en un dispositivo. Su token de renovación (en una cookie que el JavaScript no puede leer) permite
 * pedir tokens de acceso nuevos sin volver a iniciar sesión mientras se use la app. Se guarda solo su hash y cambia en
 * cada renovación; el anterior se recuerda para detectar si alguien reusa uno ya gastado (señal de robo).
 */
@Entity
@Table(name = "sesiones_cuenta")
@Getter
@NoArgsConstructor
public class SesionCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    /** SHA-256 del token de renovación vigente, en hexadecimal. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** SHA-256 del token que se reemplazó en la última renovación. */
    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    /** Inicio o última renovación: el plazo de inactividad se cuenta desde acá. */
    @Column(nullable = false)
    private LocalDateTime renovadaEn;

    @Column(nullable = false)
    private LocalDateTime venceEn;

    /** Cuándo se cerró (logout, cambio de contraseña o token reusado). Null = abierta. */
    private LocalDateTime cerradaEn;

    public SesionCuenta(Cuenta cuenta, String tokenHash, LocalDateTime ahora, LocalDateTime venceEn) {
        this.cuenta = cuenta;
        this.tokenHash = tokenHash;
        this.creadaEn = ahora;
        this.renovadaEn = ahora;
        this.venceEn = venceEn;
    }

    /** Sirve para seguir usando la app: no se cerró y no pasó el plazo de inactividad. */
    public boolean estaVigente(LocalDateTime ahora) {
        return cerradaEn == null && ahora.isBefore(venceEn);
    }

    /** Reemplaza el token de renovación y vuelve a contar el plazo de inactividad desde ahora. */
    public void renovar(String nuevoHash, LocalDateTime ahora, LocalDateTime venceEn) {
        this.hashAnterior = this.tokenHash;
        this.tokenHash = nuevoHash;
        this.renovadaEn = ahora;
        this.venceEn = venceEn;
    }

    public void cerrar(LocalDateTime ahora) {
        if (cerradaEn == null) {
            cerradaEn = ahora;
        }
    }
}
