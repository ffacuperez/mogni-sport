package mogni.system_backend.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie con el token de renovación. El JavaScript no la puede leer (httpOnly), así un script inyectado no se lleva la
 * sesión, y solo viaja a este mismo sitio (SameSite=Strict). Va en "/" porque detrás de Caddy el back está en /api y
 * no sabe con qué ruta lo ve el navegador.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "mogni_sesion";

    private final boolean segura;

    /** segura=false solo para probar sin HTTPS desde otra máquina; en la compu, localhost ya cuenta como seguro. */
    public CookieSesion(@Value("${app.sesion.cookie-segura:true}") boolean segura) {
        this.segura = segura;
    }

    public ResponseCookie crear(String token, Duration duracion) {
        return base(token).maxAge(duracion).build();
    }

    public ResponseCookie borrar() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor).httpOnly(true).secure(segura).sameSite("Strict").path("/");
    }
}
