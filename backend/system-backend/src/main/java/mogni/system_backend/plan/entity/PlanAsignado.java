package mogni.system_backend.plan.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
 * copia profunda (semanas, sesiones y ejercicios propios), así que modificar el plan no toca la plantilla y viceversa.
 * Cada semana tiene sus propias sesiones, y la cantidad de semanas es la duración del plan.
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
     * HU-266: último día del plan (inclusiva): el último día de la última semana. Se recalcula cada vez que cambian
     * las semanas (ver {@link #agregarSemanas}). Se guarda para no tener que cargar las semanas en los listados.
     */
    @Column(nullable = false)
    private LocalDate fechaFin;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SemanaPlan> semanas = new ArrayList<>();

    public PlanAsignado(Alumno alumno, LocalDate fechaAsignacion) {
        this.alumno = alumno;
        this.fechaAsignacion = fechaAsignacion;
    }

    /** Copia profunda de la plantilla para el alumno: mismo nombre, nivel, objetivo y semanas. */
    public static PlanAsignado copiaDe(PlantillaPlan plantilla, Alumno alumno, LocalDate fechaAsignacion) {
        PlanAsignado plan = new PlanAsignado(alumno, fechaAsignacion);
        plan.setNombre(plantilla.getNombre());
        plan.setNivel(plantilla.getNivel());
        plan.setObjetivo(plantilla.getObjetivo());
        plan.setPlantillaOrigen(plantilla);
        plan.agregarSemanas(plantilla.getSemanas().stream().map(SemanaPlan::copiaDe).toList());
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
        return estaActivo() && !fechaFin.isBefore(hoy);
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


    /** En qué semana del plan cae ese día: la 1 va del día de la asignación a seis días después. */
    public int semanaEn(LocalDate dia) {
        return (int) ChronoUnit.WEEKS.between(fechaAsignacion, dia) + 1;
    }

    /**
     * Sale de la fecha fin, que siempre es el último día de la última semana: así los listados no tienen que cargar
     * las semanas.
     */
    public int cantidadSemanas() {
        long dias = ChronoUnit.DAYS.between(fechaAsignacion, fechaFin) + 1;
        return (int) Math.max(1, Math.ceilDiv(dias, 7));
    }

    /** La semana en curso, entre 1 y la última. Un plan que ya no está vigente queda en su última semana. */
    public int semanaActual(LocalDate hoy) {
        int cantidad = cantidadSemanas();
        if (!estaVigente(hoy)) {
            return cantidad;
        }
        return Math.clamp(semanaEn(hoy), 1, cantidad);
    }

    /**
     * Agrega las semanas al final, en el orden recibido, siguiendo la numeración, y corre la fecha fin al último día
     * de la última semana. Para reemplazar semanas, primero hay que sacarlas y hacer flush (ver
     * PlanAsignadoService.actualizarPlan).
     */
    public void agregarSemanas(List<SemanaPlan> nuevas) {
        for (SemanaPlan semana : nuevas) {
            semana.setNumero(semanas.size() + 1);
            semana.setPlan(this);
            semanas.add(semana);
        }
        fechaFin = fechaAsignacion.plusWeeks(semanas.size()).minusDays(1);
    }

    public Optional<SemanaPlan> buscarSemana(int numero) {
        return semanas.stream().filter(semana -> semana.getNumero() == numero).findFirst();
    }

    /** Lo que el profe indicó para esa sesión de esa semana, si existe. */
    public Optional<SesionPlan> buscarSesion(int semana, int numeroSesion) {
        return buscarSemana(semana).flatMap(encontrada -> encontrada.buscarSesion(numeroSesion));
    }

    /** Todas las sesiones del plan: la suma de las de cada semana. */
    public int sesionesTotales() {
        return semanas.stream().mapToInt(semana -> semana.getSesiones().size()).sum();
    }
}
