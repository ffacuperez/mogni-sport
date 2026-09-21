package mogni.system_backend.membresia.entity;

import java.math.BigDecimal;

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

@Entity
@Table(name = "membresias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Vigencia de la membresía en días (columna preexistente en la base). */
    @Column(nullable = false)
    private Integer duracionDias;

    /** Frecuencia de entrenamiento permitida. Nullable porque hay membresías cargadas antes de este campo. */
    private Integer diasPorSemana;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
}
