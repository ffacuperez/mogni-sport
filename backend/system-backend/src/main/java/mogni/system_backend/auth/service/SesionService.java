package mogni.system_backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.entity.SesionCuenta;
import mogni.system_backend.auth.repository.SesionCuentaRepository;
import mogni.system_backend.config.ZonaHoraria;

/**
 * Sesiones largas con renovación: el token de acceso dura pocos minutos y, mientras se use la app, se pide uno nuevo
 * con el token de renovación, que también cambia en cada uso. Así el alumno no vuelve a iniciar sesión mientras
 * entrene seguido, y la sesión se puede cortar del lado del servidor. Reglas:
 * <ul>
 *   <li>la sesión vence tras {@code diasAlumno} días sin uso para los alumnos y {@code diasPersonal} para el personal,
 *       que puede ver y cambiar mucho más;</li>
 *   <li>cerrar sesión cierra la de ese dispositivo; cambiar o restablecer la contraseña cierra todas las de la cuenta;</li>
 *   <li>si alguien usa un token de renovación ya reemplazado, se cierra la sesión (alguien lo copió);</li>
 *   <li>en la base se guarda solo el hash del token.</li>
 * </ul>
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    /** Dos renovaciones casi a la vez del mismo dispositivo (p. ej. dos pestañas) no se toman como robo. */
    static final long SEGUNDOS_GRACIA = 30;

    /** Lo que se le entrega al dispositivo: la sesión y su token de renovación en claro (solo viaja en la cookie). */
    public record SesionAbierta(SesionCuenta sesion, String token) {
    }

    private final SesionCuentaRepository sesionRepository;
    private final int diasAlumno;
    private final int diasPersonal;
    private final SecureRandom random = new SecureRandom();

    public SesionService(SesionCuentaRepository sesionRepository,
                         @Value("${app.sesion.dias-alumno:45}") int diasAlumno,
                         @Value("${app.sesion.dias-personal:14}") int diasPersonal) {
        this.sesionRepository = sesionRepository;
        this.diasAlumno = diasAlumno;
        this.diasPersonal = diasPersonal;
    }

    @Transactional
    public SesionAbierta iniciar(Cuenta cuenta) {
        return iniciar(cuenta, ZonaHoraria.ahora());
    }

    SesionAbierta iniciar(Cuenta cuenta, LocalDateTime ahora) {
        String token = generarToken();
        SesionCuenta sesion = sesionRepository.save(
                new SesionCuenta(cuenta, hash(token), ahora, ahora.plus(duracion(cuenta))));
        return new SesionAbierta(sesion, token);
    }

    /** Cambia el token de renovación por uno nuevo. El cierre por token reusado se guarda aunque se rechace el pedido. */
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public SesionAbierta renovar(String token) {
        return renovar(token, ZonaHoraria.ahora());
    }

    SesionAbierta renovar(String token, LocalDateTime ahora) {
        if (token == null || token.isBlank()) {
            throw new SesionInvalidaException();
        }
        String hash = hash(token);
        Optional<SesionCuenta> encontrada = sesionRepository.findByTokenHash(hash);
        if (encontrada.isEmpty()) {
            sesionRepository.findByHashAnterior(hash).ifPresent(sesion -> cerrarSiFueReusado(sesion, ahora));
            throw new SesionInvalidaException();
        }
        SesionCuenta sesion = encontrada.get();
        if (!sesion.estaVigente(ahora) || !sesion.getCuenta().isActiva()) {
            throw new SesionInvalidaException();
        }
        String nuevo = generarToken();
        sesion.renovar(hash(nuevo), ahora, ahora.plus(duracion(sesion.getCuenta())));
        return new SesionAbierta(sesionRepository.save(sesion), nuevo);
    }

    /** Cierra la sesión de ese token de renovación, si existe (logout). */
    @Transactional
    public void cerrar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sesionRepository.findByTokenHash(hash(token)).ifPresent(this::cerrarSesion);
    }

    /** Cierra esa sesión, si existe (logout con el token de acceso). */
    @Transactional
    public void cerrarPorId(Long sesionId) {
        if (sesionId != null) {
            sesionRepository.findById(sesionId).ifPresent(this::cerrarSesion);
        }
    }

    /** Cierra todas las sesiones abiertas de la cuenta, en todos los dispositivos. */
    @Transactional
    public void cerrarTodas(Cuenta cuenta) {
        sesionRepository.findByCuentaIdAndCerradaEnIsNull(cuenta.getId()).forEach(this::cerrarSesion);
    }

    /** Si el token de acceso de esa sesión todavía puede usarse (lo consulta el filtro en cada pedido). */
    @Transactional(readOnly = true)
    public boolean estaVigente(Long sesionId) {
        return sesionRepository.findById(sesionId).filter(sesion -> sesion.estaVigente(ZonaHoraria.ahora())).isPresent();
    }

    /** Cuánto dura la sesión sin uso: también es la vida de la cookie. */
    public Duration duracion(Cuenta cuenta) {
        return Duration.ofDays(cuenta.getRol() == Rol.ALUMNO ? diasAlumno : diasPersonal);
    }

    private void cerrarSesion(SesionCuenta sesion) {
        sesion.cerrar(ZonaHoraria.ahora());
        sesionRepository.save(sesion);
    }

    private void cerrarSiFueReusado(SesionCuenta sesion, LocalDateTime ahora) {
        if (sesion.getCerradaEn() == null && sesion.getRenovadaEn().isBefore(ahora.minusSeconds(SEGUNDOS_GRACIA))) {
            sesion.cerrar(ahora);
            sesionRepository.save(sesion);
            log.warn("Se reusó un token de renovación ya reemplazado en la sesión {}: se cierra", sesion.getId());
        }
    }

    /** 256 bits aleatorios, en Base64 apto para cookies. */
    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
