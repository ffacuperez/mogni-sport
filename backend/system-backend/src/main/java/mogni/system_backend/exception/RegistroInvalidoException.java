package mogni.system_backend.exception;

public class RegistroInvalidoException extends RuntimeException {
    public RegistroInvalidoException(String mensaje) {
        super(mensaje);
    }
}