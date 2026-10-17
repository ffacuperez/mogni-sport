package mogni.system_backend.turno.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-252: parámetros generales del gimnasio. Siempre hay una sola fila (la crea
 * {@link mogni.system_backend.turno.service.InicializacionConfiguracionGimnasio}).
 * Por ahora solo guarda el cupo máximo por turno, que los turnos copian al generarse.
 */
@Entity
@Table(name = "configuracion_gimnasio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionGimnasio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int cupoMaximo;
}
