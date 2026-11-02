package mogni.system_backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mogni.system_backend.auth.dto.CambiarPasswordRequest;
import mogni.system_backend.auth.dto.CambiarPasswordResponse;
import mogni.system_backend.auth.dto.LoginRequest;
import mogni.system_backend.auth.dto.LoginResponse;
import mogni.system_backend.auth.dto.MensajeResponse;
import mogni.system_backend.auth.dto.RestablecerPasswordRequest;
import mogni.system_backend.auth.dto.SolicitarRecuperacionRequest;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.service.CuentaService;
import mogni.system_backend.auth.service.RecuperacionPasswordService;
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
    private final RecuperacionPasswordService recuperacionPasswordService;

    public AuthController(AuthenticationManager authenticationManager,
                           CuentaRepository cuentaRepository,
                           JwtService jwtService,
                           CuentaService cuentaService,
                           RecuperacionPasswordService recuperacionPasswordService) {
        this.authenticationManager = authenticationManager;
        this.cuentaRepository = cuentaRepository;
        this.jwtService = jwtService;
        this.cuentaService = cuentaService;
        this.recuperacionPasswordService = recuperacionPasswordService;
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

    /**
     * HU-248 (primer inicio) y MOGNI-263 (desde el perfil, cualquier rol): requiere estar logueado (ver
     * SecurityConfig). Devuelve un token nuevo sin la marca de cambio pendiente, así la sesión sigue abierta.
     */
    @PostMapping("/cambiar-password")
    public ResponseEntity<CambiarPasswordResponse> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        Cuenta cuenta = cuentaService.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.ok(new CambiarPasswordResponse(
                "La contraseña se cambió correctamente", jwtService.generarToken(cuenta)));
    }

    /**
     * Recuperar contraseña, paso 1 (sin sesión): envía por mail un link para restablecerla. Responde siempre lo
     * mismo, exista o no el email, para no revelar qué cuentas existen.
     */
    @PostMapping("/recuperar-password")
    public ResponseEntity<MensajeResponse> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        recuperacionPasswordService.solicitar(request.getEmail());
        return ResponseEntity.ok(new MensajeResponse(RecuperacionPasswordService.MENSAJE_SOLICITUD));
    }

    /** Recuperar contraseña: 200 si el link del mail todavía sirve, 400 si venció o ya se usó. */
    @GetMapping("/recuperar-password/validar")
    public ResponseEntity<MensajeResponse> validarLinkRecuperacion(@RequestParam(required = false) String token) {
        recuperacionPasswordService.validar(token);
        return ResponseEntity.ok(new MensajeResponse("El link es válido"));
    }

    /** Recuperar contraseña, paso 2 (sin sesión): guarda la contraseña nueva con el token del link del mail. */
    @PostMapping("/restablecer-password")
    public ResponseEntity<MensajeResponse> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacionPasswordService.restablecer(request);
        return ResponseEntity.ok(new MensajeResponse("Tu contraseña se restableció. Ya podés iniciar sesión."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Con JWT stateless, "cerrar sesión" del lado backend no invalida el token en sí;
        // el frontend simplemente borra el token guardado. Dejamos el endpoint para que
        // el flujo del frontend sea explícito (y por si más adelante suman una lista negra de tokens).
        return ResponseEntity.noContent().build();
    }
}