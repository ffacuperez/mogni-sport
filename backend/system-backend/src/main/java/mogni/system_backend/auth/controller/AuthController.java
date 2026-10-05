package mogni.system_backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.auth.dto.CambiarPasswordRequest;
import mogni.system_backend.auth.dto.CambiarPasswordResponse;
import mogni.system_backend.auth.dto.LoginRequest;
import mogni.system_backend.auth.dto.LoginResponse;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.service.CuentaService;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CuentaRepository cuentaRepository;
    private final JwtService jwtService;
    private final CuentaService cuentaService;

    public AuthController(AuthenticationManager authenticationManager,
                           CuentaRepository cuentaRepository,
                           JwtService jwtService,
                           CuentaService cuentaService) {
        this.authenticationManager = authenticationManager;
        this.cuentaRepository = cuentaRepository;
        this.jwtService = jwtService;
        this.cuentaService = cuentaService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));

        String token = jwtService.generarToken(cuenta);

        return ResponseEntity.ok(new LoginResponse(token, cuenta.getRol(), cuenta.isDebeCambiarPassword()));
    }

    /** HU-248: requiere estar logueado (ver SecurityConfig). Devuelve un token nuevo sin la marca de cambio pendiente. */
    @PostMapping("/cambiar-password")
    public ResponseEntity<CambiarPasswordResponse> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        Cuenta cuenta = cuentaService.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.ok(new CambiarPasswordResponse(
                "La contraseña se cambió correctamente", jwtService.generarToken(cuenta)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Con JWT stateless, "cerrar sesión" del lado backend no invalida el token en sí;
        // el frontend simplemente borra el token guardado. Dejamos el endpoint para que
        // el flujo del frontend sea explícito (y por si más adelante suman una lista negra de tokens).
        return ResponseEntity.noContent().build();
    }
}