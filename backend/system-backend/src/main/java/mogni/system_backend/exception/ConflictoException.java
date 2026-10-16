package mogni.system_backend.exception;

/** La solicitud es válida pero choca con el estado actual de los datos: sin lugares, ya reservado, etc. (409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
