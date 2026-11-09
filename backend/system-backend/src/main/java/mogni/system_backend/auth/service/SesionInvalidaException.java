package mogni.system_backend.auth.service;

/** El token de renovación no sirve (no existe, venció, se cerró o es de una cuenta dada de baja): hay que iniciar sesión. */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesión venció. Volvé a iniciar sesión.");
    }
}
