package mogni.system_backend.progreso.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mogni.system_backend.plan.entity.MetodoPlan;

/** HU-270: una sesión registrada, con lo indicado y lo realizado en cada ejercicio. */
@Getter
@AllArgsConstructor
public class SesionDetalleResponse {
    private Long id;
    private int semana;
    private int numeroSesion;
    /** El nombre de la sesión en el plan (p. ej. "Tren inferior A"); null si no tiene. */
    private String nombreSesion;
    private LocalDate fecha;
    private String nombrePlan;
    /** Null en los planes que no lo tienen cargado, igual que mesociclo. */
    private MetodoPlan metodo;
    private String mesociclo;
    private List<EjercicioRealizadoResponse> ejercicios;
}
