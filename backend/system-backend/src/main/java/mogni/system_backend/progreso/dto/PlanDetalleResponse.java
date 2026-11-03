package mogni.system_backend.progreso.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.MetodoPlan;

/** HU-270: el plan como lo ve el alumno, con sus semanas y sesiones. */
@Getter
@AllArgsConstructor
public class PlanDetalleResponse {
    private Long id;
    private String nombre;
    /** Null en los planes que no lo tienen cargado, igual que mesociclo y objetivoMesociclo. */
    private MetodoPlan metodo;
    private String mesociclo;
    private String objetivoMesociclo;
    private EstadoPlanAsignado estado;
    private LocalDate fechaInicio;
    /** Activo: el fin previsto. Finalizado: el día en que terminó. */
    private LocalDate fechaFin;
    private int cantidadSemanas;
    /** La semana en curso según la fecha de hoy; en un plan finalizado, la última. */
    private int semanaActual;
    private int sesionesRegistradas;
    private int sesionesTotales;
    private List<SemanaPlanResponse> semanas;
}
