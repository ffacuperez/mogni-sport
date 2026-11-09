package mogni.system_backend.membresia.entity;

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

    /** Rango de días por semana que puede tener una membresía (lo mismo valida el formulario del frontend). */
    public static final int DIAS_POR_SEMANA_MIN = 2;
    public static final int DIAS_POR_SEMANA_MAX = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(nullable = false)
    private double precioActual;

    @Column(nullable = false)
    private int frecuenciaSemanal;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
