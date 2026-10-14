package mogni.system_backend.notificacion.entity;

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
import mogni.system_backend.auth.entity.Cuenta;

/**
 * Un dispositivo (navegador o app instalada) que aceptó recibir notificaciones push. Una cuenta puede tener
 * varios (celular y compu). Los datos los genera el navegador al aceptar el permiso.
 */
@Entity
@Table(name = "suscripciones_push")
@Getter
@Setter
@NoArgsConstructor
public class SuscripcionPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    /** URL del servicio de push del navegador (Google, Apple, Mozilla) para este dispositivo. */
    @Column(nullable = false, unique = true, length = 1024)
    private String endpoint;

    /** Clave pública del dispositivo, para cifrar el mensaje (base64url). */
    @Column(nullable = false, length = 200)
    private String p256dh;

    /** Secreto del dispositivo, también para el cifrado (base64url). */
    @Column(nullable = false, length = 100)
    private String auth;

    @Column(nullable = false)
    private LocalDateTime fechaAlta;
}
