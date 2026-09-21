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

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generarToken(Cuenta cuenta) {
        return Jwts.builder()
                .subject(cuenta.getEmail())
                .claim("rol", cuenta.getRol().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(obtenerClave())
                .compact();
    }

    public String extraerEmail(String token) {
        return parsearClaims(token).getSubject();
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