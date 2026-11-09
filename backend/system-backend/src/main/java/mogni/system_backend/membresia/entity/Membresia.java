package mogni.system_backend.membresia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membresias")
@Getter
@Setter
@NoArgsConstructor
public class Membresia {

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

    @Column(nullable = true)
    private Boolean activa = true;

    // Constructor compatible con pruebas anteriores de 5 argumentos
    public Membresia(Long id, String nombre, double precioActual, int frecuenciaSemanal, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.precioActual = precioActual;
        this.frecuenciaSemanal = frecuenciaSemanal;
        this.descripcion = descripcion;
        this.activa = true;
    }
}