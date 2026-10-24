package mogni.system_backend.alumno.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.turno.service.CancelacionPorSistemaService;

/**
 * Baja del alumno (MOGNI-246), la misma para la baja manual del propietario y la automática por mora: la cuenta queda
 * inactiva (no puede iniciar sesión), se cancela la inscripción activa y sus turnos que todavía no empezaron. Está
 * aparte de AlumnoService para que la use también la puesta al día sin dependencias circulares.
 */
@Service
public class BajaAlumnoService {

    private final CancelacionPorSistemaService cancelacionPorSistemaService;

    public BajaAlumnoService(CancelacionPorSistemaService cancelacionPorSistemaService) {
        this.cancelacionPorSistemaService = cancelacionPorSistemaService;
    }

    /** No hace nada si el alumno ya estaba inactivo. Se usa dentro de la transacción de quien la llama. */
    public void darDeBaja(Alumno alumno, LocalDateTime ahora) {
        if (!alumno.getCuenta().isActiva()) {
            return;
        }
        alumno.getCuenta().setActiva(false);
        alumno.getCuenta().setFechaBaja(ahora.toLocalDate());
        alumno.buscarInscripcionActiva().ifPresent(inscripcion -> inscripcion.cancelar(ahora.toLocalDate()));
        // Un cambio de membresía ya pagado para el mes siguiente también se cancela, sin devolución (MP5, 20.10)
        alumno.buscarInscripcionProgramada().ifPresent(inscripcion -> inscripcion.cancelar(ahora.toLocalDate()));
        cancelacionPorSistemaService.cancelarReservasFuturas(alumno.getId(), ahora);
    }
}
