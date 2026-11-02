package mogni.system_backend.plantilla.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-265: plantilla de plan de entrenamiento que el entrenador arma una vez y reutiliza al asignarla a
 * distintos alumnos. Tiene una o más sesiones numeradas, cada una con ejercicios del catálogo.
 * Eliminar es una baja lógica (activa = false): la plantilla deja de listarse pero no se borra.
 */
@Entity
@Table(name = "plantillas_plan")
@Getter
@Setter
@NoArgsConstructor
public class PlantillaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * El nombre en minúsculas mientras está activa y null al darla de baja, que así libera su nombre.
     * El índice único es lo que frena dos altas simultáneas que pasan juntas la validación del servicio.
     */
    @Column(unique = true, length = 150)
    private String nombreUnico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelPlantilla nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObjetivoPlantilla objetivo;

    @Column(nullable = false)
    private boolean activa = true;

    /**
     * Alumnos que tienen un plan ACTIVO copiado de esta plantilla (HU-266): suma al asignar y resta al
     * finalizar el plan. Mientras sea mayor a 0, la plantilla no se puede eliminar.
     */
    @Column(nullable = false)
    private int asignacionesActivas = 0;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SesionPlantilla> sesiones = new ArrayList<>();

    public void setNombre(String nombre) {
        this.nombre = nombre;
        actualizarNombreUnico();
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
        actualizarNombreUnico();
    }

    private void actualizarNombreUnico() {
        nombreUnico = activa && nombre != null ? nombre.toLowerCase(Locale.ROOT) : null;
    }

    public boolean tieneAsignacionesActivas() {
        return asignacionesActivas > 0;
    }

    /** Reemplaza todas las sesiones y las numera 1, 2, 3... en el orden recibido. */
    public void reemplazarSesiones(List<SesionPlantilla> nuevas) {
        sesiones.clear();
        for (int i = 0; i < nuevas.size(); i++) {
            SesionPlantilla sesion = nuevas.get(i);
            sesion.setNumero(i + 1);
            sesion.setPlantilla(this);
            sesiones.add(sesion);
        }
    }

    /** Copia independiente: sesiones y ejercicios nuevos, sin asignaciones. Modificarla no toca la original. */
    public PlantillaPlan duplicar(String nombreCopia, LocalDateTime ahora) {
        PlantillaPlan copia = new PlantillaPlan();
        copia.setNombre(nombreCopia);
        copia.setNivel(nivel);
        copia.setObjetivo(objetivo);
        copia.setFechaActualizacion(ahora);
        copia.reemplazarSesiones(sesiones.stream().map(SesionPlantilla::copiar).toList());
        return copia;
    }
}
