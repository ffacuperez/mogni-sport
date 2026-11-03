package mogni.system_backend.plan.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.plantilla.entity.PlantillaPlan;

/**
 * HU-266: plan de entrenamiento de un alumno. Se arma desde cero o copiando una plantilla; en ese caso es una
 * copia profunda (sesiones y ejercicios propios), así que modificar el plan no toca la plantilla y viceversa.
 */
@Entity
@Table(name = "planes_asignados")
@Getter
@Setter
@NoArgsConstructor
public class PlanAsignado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelPlantilla nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObjetivoPlantilla objetivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPlanAsignado estado = EstadoPlanAsignado.ACTIVO;

    /**
     * De qué plantilla salió, o null si se armó desde cero. Solo sirve para saber el origen y llevar la cuenta
     * de asignaciones activas de la plantilla: los datos del plan son una copia y no se leen de acá.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_origen_id")
    private PlantillaPlan plantillaOrigen;

    @Column(nullable = false)
    private LocalDate fechaAsignacion;

    /** Null mientras está ACTIVO. */
    private LocalDate fechaFinalizacion;

    /**
     * HU-266: último día del plan (inclusiva). Al asignar es obligatoria, pero la columna admite null por los planes
     * anteriores a esta regla: un plan sin fecha fin sigue vigente hasta que se lo finalice a mano.
     */
    private LocalDate fechaFin;

    /** HU-270: null en los planes que no lo tienen cargado, igual que los tres campos siguientes. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MetodoPlan metodo;

    /** Hipertrofia, Fuerza, Reset/Técnica... La lista es abierta. */
    @Column(length = 60)
    private String mesociclo;

    @Column(length = 150)
    private String objetivoMesociclo;

    /** Las sesiones del plan se repiten igual cada semana. */
    private Integer cantidadSemanas;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SesionPlan> sesiones = new ArrayList<>();

    public PlanAsignado(Alumno alumno, LocalDate fechaAsignacion) {
        this.alumno = alumno;
        this.fechaAsignacion = fechaAsignacion;
    }

    /** Copia profunda de la plantilla para el alumno: mismo nombre, nivel, objetivo y sesiones. */
    public static PlanAsignado copiaDe(PlantillaPlan plantilla, Alumno alumno, LocalDate fechaAsignacion) {
        PlanAsignado plan = new PlanAsignado(alumno, fechaAsignacion);
        plan.setNombre(plantilla.getNombre());
        plan.setNivel(plantilla.getNivel());
        plan.setObjetivo(plantilla.getObjetivo());
        plan.setPlantillaOrigen(plantilla);
        plan.reemplazarSesiones(plantilla.getSesiones().stream().map(SesionPlan::copiaDe).toList());
        return plan;
    }

    /** Solo mira el estado guardado. Para saber si el alumno lo tiene como plan actual, usar {@link #estaVigente}. */
    public boolean estaActivo() {
        return estado == EstadoPlanAsignado.ACTIVO;
    }

    /**
     * HU-266: la única definición de "plan activo" del alumno. ACTIVO y sin vencer: la fecha fin es inclusiva, así que
     * ese día todavía es vigente y desde el siguiente ya no, sin que nadie lo finalice a mano.
     */
    public boolean estaVigente(LocalDate hoy) {
        return estaActivo() && (fechaFin == null || !fechaFin.isBefore(hoy));
    }

    /** ACTIVO y con la fecha fin ya pasada: se muestra como finalizado aunque la puesta al día todavía no lo cerró. */
    public boolean estaVencido(LocalDate hoy) {
        return estaActivo() && !estaVigente(hoy);
    }

    /** El estado que ve la gente: un plan vencido figura FINALIZADO. */
    public EstadoPlanAsignado estadoAl(LocalDate hoy) {
        return estaVigente(hoy) ? EstadoPlanAsignado.ACTIVO : EstadoPlanAsignado.FINALIZADO;
    }

    /** Un plan vencido terminó el día de su fecha fin. */
    public LocalDate fechaFinalizacionAl(LocalDate hoy) {
        return estaVencido(hoy) ? fechaFin : fechaFinalizacion;
    }

    public void finalizar(LocalDate fecha) {
        estado = EstadoPlanAsignado.FINALIZADO;
        fechaFinalizacion = fecha;
    }

    /** Reemplaza todas las sesiones y las numera 1, 2, 3... en el orden recibido. */
    public void reemplazarSesiones(List<SesionPlan> nuevas) {
        sesiones.clear();
        for (int i = 0; i < nuevas.size(); i++) {
            SesionPlan sesion = nuevas.get(i);
            sesion.setNumero(i + 1);
            sesion.setPlan(this);
            sesiones.add(sesion);
        }
    }
}
