package mogni.system_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.dto.LoginRequest;
import mogni.system_backend.dto.LoginResponse;
import mogni.system_backend.entidades.Cuenta;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.repository.CuentaRepository;
import mogni.system_backend.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CuentaRepository cuentaRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                           CuentaRepository cuentaRepository,
                           JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.cuentaRepository = cuentaRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Cuenta cuenta = cuentaRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));

        String token = jwtService.generarToken(cuenta);

        return ResponseEntity.ok(new LoginResponse(token, cuenta.getRol(), cuenta.isDebeCambiarPassword()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Con JWT stateless, "cerrar sesión" del lado backend no invalida el token en sí;
        // el frontend simplemente borra el token guardado. Dejamos el endpoint para que
        // el flujo del frontend sea explícito (y por si más adelante suman una lista negra de tokens).
        return ResponseEntity.noContent().build();
    }
}