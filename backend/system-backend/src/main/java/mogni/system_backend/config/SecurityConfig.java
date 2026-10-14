package mogni.system_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import mogni.system_backend.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sin token válido respondemos 401 (no el 403 por defecto) para que el frontend pueda redirigir al login
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // El re-despacho interno a /error (sendError) llega sin el JWT procesado; si no se permite,
                // un 403 por falta de rol termina respondiéndose como 401 y el frontend cierra la sesión
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // Cambiar la contraseña requiere estar logueado; va antes del permitAll de /auth/**
                .requestMatchers("/auth/cambiar-password").authenticated()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // El alumno solo puede ver sus propios datos; va antes de /alumnos/** para que tenga prioridad
                .requestMatchers(HttpMethod.GET, "/alumnos/me").hasRole("ALUMNO")
                // Notificaciones de cuota (HU-250): cada alumno ve solo las suyas
                .requestMatchers("/notificaciones/**").hasRole("ALUMNO")
                // Historial de pagos (HU-48): solo el propietario, también antes de /alumnos/**
                .requestMatchers(HttpMethod.GET, "/alumnos/*/pagos").hasRole("PROPIETARIO")
                .requestMatchers("/alumnos/**", "/membresias/**")
                    .hasAnyRole("PROPIETARIO", "ENTRENADOR", "NUTRICIONISTA")
                // Cobranzas: solo el propietario
                .requestMatchers("/cuotas/**", "/pagos/**").hasRole("PROPIETARIO")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
