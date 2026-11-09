package mogni.system_backend.alumno.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.cuota.dto.CuotaAlumnoResumen;
import mogni.system_backend.inscripcion.dto.InscripcionResponse;

@Getter
@AllArgsConstructor
public class AlumnoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private int edad;
    private String telefono;
    private String sexo;
    private String dni;
    private String email;
    private LocalDate fechaNacimiento;
    private String objetivo;
    private String cuestionarioSalud;
    private Rol rol;
    private boolean activo;
    private LocalDate fechaBaja;
    private Long membresiaId;
    private String membresiaNombre;
    /** Inicio de la inscripción activa (null si no tiene). */
    private LocalDate fechaInicioInscripcion;
    /** Inscripción activa o, si no tiene, la última (con su estado). Null si nunca tuvo. */
    private InscripcionResponse inscripcionActual;
    /** Solo para alumnos inactivos: vencimiento de su última cuota (de cualquier inscripción); null si no tiene. */
    private LocalDate vencimientoUltimaCuota;
    /**
     * Solo para alumnos inactivos: true si al reactivarlo hay que elegir membresía y cobrar la primera cuota; false si
     * su última cuota todavía cubre hoy y se reactiva su misma inscripción. Null si está activo.
     */
    private Boolean reactivacionRequierePago;
    /** Membresía ya pagada para el período siguiente (cambio programado); null si no tiene. */
    private String membresiaProgramada;
    /** Desde qué día rige la membresía programada; null si no tiene. */
    private LocalDate programadaDesde;
    /** Plan de entrenamiento ACTIVO (a lo sumo uno); null si no tiene. Se calcula al vuelo, no se guarda en Alumno. */
    private PlanActivoResumen planActivo;
    /** Situación de cuota; solo para alumnos activos (null si está inactivo). */
    private CuotaAlumnoResumen cuotaActual;
}