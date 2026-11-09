package mogni.system_backend.turno.service;

/** El propietario canceló el turno de un alumno: se le avisa por push con el mismo texto que el aviso de la app. */
public record TurnoCanceladoPorGimnasioEvent(Long cuentaId, Long avisoId, String titulo, String mensaje) {
}
