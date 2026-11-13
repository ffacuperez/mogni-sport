package mogni.system_backend.plantilla.service;

import java.util.List;

import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plantilla.dto.SemanaRequest;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.entity.SemanaPlantilla;

/**
 * Las reglas de las semanas de una plantilla y de un plan asignado: de 1 a 24 semanas, de 1 a 7 sesiones por semana,
 * al menos un ejercicio por sesión y nota de hasta 60 caracteres. Son las mismas que validan SemanaRequest y los
 * requests de plantilla y plan; se repiten acá por si el servicio se usa sin pasar por el controller.
 */
public final class ReglasSemanas {

    private ReglasSemanas() {
    }

    /** queEs: "La plantilla" o "El plan", para el mensaje. */
    public static void validar(List<SemanaRequest> semanas, String queEs) {
        if (semanas == null || semanas.isEmpty()) {
            throw new RegistroInvalidoException(queEs + " tiene que tener al menos una semana");
        }
        if (semanas.size() > PlantillaPlan.MAXIMO_SEMANAS) {
            throw new RegistroInvalidoException(queEs + " puede tener como máximo 24 semanas");
        }
        for (SemanaRequest semana : semanas) {
            if (semana == null || semana.getSesiones() == null || semana.getSesiones().isEmpty()) {
                throw new RegistroInvalidoException("Cada semana tiene que tener al menos una sesión");
            }
            if (semana.getSesiones().size() > PlantillaPlan.MAXIMO_SESIONES_POR_SEMANA) {
                throw new RegistroInvalidoException("Cada semana puede tener como máximo 7 sesiones");
            }
            if (semana.getSesiones().stream().anyMatch(ReglasSemanas::sinEjercicios)) {
                throw new RegistroInvalidoException("Cada sesión tiene que tener al menos un ejercicio");
            }
            String nota = CatalogoEjercicios.textoOpcional(semana.getNota());
            if (nota != null && nota.length() > SemanaPlantilla.NOTA_MAX) {
                throw new RegistroInvalidoException("La nota de la semana no puede superar los 60 caracteres");
            }
        }
    }

    /** Todas las sesiones de esas semanas, en orden. */
    public static List<SesionRequest> sesiones(List<SemanaRequest> semanas) {
        return semanas.stream().flatMap(semana -> semana.getSesiones().stream()).toList();
    }

    private static boolean sinEjercicios(SesionRequest sesion) {
        return sesion == null || sesion.getEjercicios() == null || sesion.getEjercicios().isEmpty();
    }
}
