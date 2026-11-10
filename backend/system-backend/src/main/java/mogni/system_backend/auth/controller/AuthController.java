package mogni.system_backend.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.JwtException;
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
import mogni.system_backend.auth.service.SesionInvalidaException;
import mogni.system_backend.auth.service.SesionService;
import mogni.system_backend.auth.service.SesionService.SesionAbierta;
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
    private final SesionService sesionService;
    private final CookieSesion cookieSesion;

    public AuthController(AuthenticationManager authenticationManager,
                           CuentaRepository cuentaRepository,
                           JwtService jwtService,
                           CuentaService cuentaService,
                           RecuperacionPasswordService recuperacionPasswordService,
                           SesionService sesionService,
                           CookieSesion cookieSesion) {
        this.authenticationManager = authenticationManager;
        this.cuentaRepository = cuentaRepository;
        this.jwtService = jwtService;
        this.cuentaService = cuentaService;
        this.recuperacionPasswordService = recuperacionPasswordService;
        this.sesionService = sesionService;
        this.cookieSesion = cookieSesion;
    }

    /** Abre una sesión en este dispositivo: el token de acceso va en el cuerpo y el de renovación en la cookie. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        Cuenta cuenta = cuentaRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));
        SesionAbierta sesion = sesionService.iniciar(cuenta);
        return conCookie(sesion).body(loginResponse(cuenta, sesion));
    }

    /**
     * Token de acceso nuevo con la cookie de renovación, que también se reemplaza. Sin cookie válida responde 401 y la
     * borra: hay que volver a iniciar sesión.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> renovar(
            @CookieValue(name = CookieSesion.NOMBRE, required = false) String tokenRenovacion) {
        try {
            SesionAbierta sesion = sesionService.renovar(tokenRenovacion);
            return conCookie(sesion).body(loginResponse(sesion.sesion().getCuenta(), sesion));
        } catch (SesionInvalidaException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString())
                    .build();
        }
    }

    /**
     * HU-248 (primer inicio) y MOGNI-263 (desde el perfil, cualquier rol): requiere estar logueado (ver
     * SecurityConfig). Cierra la sesión en todos los dispositivos y abre una nueva en este, así sigue adentro.
     */
    @PostMapping("/cambiar-password")
    public ResponseEntity<CambiarPasswordResponse> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        Cuenta cuenta = cuentaService.cambiarPassword(authentication.getName(), request);
        sesionService.cerrarTodas(cuenta);
        SesionAbierta sesion = sesionService.iniciar(cuenta);
        return conCookie(sesion).body(new CambiarPasswordResponse(
                "La contraseña se cambió correctamente", jwtService.generarToken(cuenta, sesion.sesion().getId())));
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

    /**
     * Recuperar contraseña, paso 2 (sin sesión): guarda la contraseña nueva con el token del link del mail y cierra la
     * sesión en todos los dispositivos (si alguien más la tenía abierta, deja de tenerla).
     */
    @PostMapping("/restablecer-password")
    public ResponseEntity<MensajeResponse> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        Cuenta cuenta = recuperacionPasswordService.restablecer(request);
        sesionService.cerrarTodas(cuenta);
        return ResponseEntity.ok(new MensajeResponse("Tu contraseña se restableció. Ya podés iniciar sesión."));
    }

    /** Cierra la sesión de este dispositivo: la de la cookie y, si viene, la del token de acceso. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieSesion.NOMBRE, required = false) String tokenRenovacion,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        sesionService.cerrar(tokenRenovacion);
        sesionService.cerrarPorId(sesionDelToken(authorization));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString())
                .build();
    }

    private LoginResponse loginResponse(Cuenta cuenta, SesionAbierta sesion) {
        return new LoginResponse(jwtService.generarToken(cuenta, sesion.sesion().getId()), cuenta.getRol(),
                cuenta.isDebeCambiarPassword());
    }

    private ResponseEntity.BodyBuilder conCookie(SesionAbierta sesion) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                cookieSesion.crear(sesion.token(), sesionService.duracion(sesion.sesion().getCuenta())).toString());
    }

    /** La sesión del token de acceso (aunque haya vencido no importa: se cierra igual), o null. */
    private Long sesionDelToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtService.extraerSesionId(authorization.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
