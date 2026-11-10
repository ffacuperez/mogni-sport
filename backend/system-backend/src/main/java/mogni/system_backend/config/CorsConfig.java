package mogni.system_backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS para el frontend. Spring Security lo toma automáticamente desde el bean
 * {@code corsConfigurationSource} (ver {@link SecurityConfig}), así que los preflight OPTIONS
 * se resuelven antes de exigir autenticación.
 */
@Configuration
public class CorsConfig {

    // Los fronts de la compu. En el servidor no hace falta: Caddy sirve front y back en el mismo dominio. Se puede
    // sobrescribir con la variable de entorno APP_CORS_ALLOWED_ORIGINS (separada por comas)
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5176,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // La cookie de sesión (ver CookieSesion) tiene que viajar también cuando el front corre en otro puerto
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
