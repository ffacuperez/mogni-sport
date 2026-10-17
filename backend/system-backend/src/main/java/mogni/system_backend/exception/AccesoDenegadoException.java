package mogni.system_backend.exception;

/** El usuario está logueado y tiene el rol correcto, pero no cumple una condición para usar la funcionalidad (403). */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
