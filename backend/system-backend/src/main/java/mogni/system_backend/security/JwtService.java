package mogni.system_backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import mogni.system_backend.auth.entity.Cuenta;

/**
 * Token de acceso: corto (pocos minutos) y atado a una sesión ("sid", ver SesionService). Cuando vence, el front pide
 * otro con el token de renovación; si la sesión se cierra, deja de servir aunque no haya vencido.
 */
@Service
public class JwtService {

    private static final String CLAIM_SESION = "sid";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${app.sesion.minutos-token-acceso:15}")
    private long minutosTokenAcceso;

    public String generarToken(Cuenta cuenta, Long sesionId) {
        return Jwts.builder()
                .subject(cuenta.getEmail())
                .claim("rol", cuenta.getRol().name())
                .claim("debeCambiarPassword", cuenta.isDebeCambiarPassword())
                .claim(CLAIM_SESION, sesionId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + minutosTokenAcceso * 60_000))
                .signWith(obtenerClave())
                .compact();
    }

    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    /** La sesión del token, o null si no tiene (tokens anteriores a las sesiones: ya no sirven). */
    public Long extraerSesionId(String token) {
        Number sesionId = parsearClaims(token).get(CLAIM_SESION, Number.class);
        return sesionId == null ? null : sesionId.longValue();
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        String email = extraerEmail(token);
        return email.equals(userDetails.getUsername()) && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return parsearClaims(token).getExpiration().before(new Date());
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerClave() {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
