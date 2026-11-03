package mogni.system_backend.mail;

/** El mail no se pudo enviar (Brevo sin configurar, caído o que rechazó el envío). */
public class MailNoEnviadoException extends RuntimeException {

    public MailNoEnviadoException(String mensaje) {
        super(mensaje);
    }

    public MailNoEnviadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
