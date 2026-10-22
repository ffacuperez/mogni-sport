package mogni.system_backend.alumno.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;
import mogni.system_backend.alumno.dto.AlumnoResponse;
import mogni.system_backend.alumno.dto.CambiarMembresiaRequest;
import mogni.system_backend.alumno.dto.CambioMembresiaResponse;
import mogni.system_backend.alumno.dto.ReactivarAlumnoRequest;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.cuota.entity.SituacionPago;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.dto.InscripcionResponse;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.repository.CuentaRepository;

@Service
public class AlumnoService {

    private static final int EDAD_MINIMA = 16;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal SIN_RECARGO = BigDecimal.ZERO.setScale(2);

    private final AlumnoRepository alumnoRepository;
    private final CuentaRepository cuentaRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoRepository pagoRepository;
    private final BajaAlumnoService bajaAlumnoService;
    private final PasswordEncoder passwordEncoder;

    public AlumnoService(AlumnoRepository alumnoRepository,
                          CuentaRepository cuentaRepository,
                          MembresiaRepository membresiaRepository,
                          PagoRepository pagoRepository,
                          BajaAlumnoService bajaAlumnoService,
                          PasswordEncoder passwordEncoder) {
        this.alumnoRepository = alumnoRepository;
        this.cuentaRepository = cuentaRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoRepository = pagoRepository;
        this.bajaAlumnoService = bajaAlumnoService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * MOGNI-73: el alta siempre se hace con la primera cuota pagada. En la misma transacción se crean el alumno, su
     * cuenta, la inscripción, la primera cuota (hoy → hoy + 30, al precio vigente) y su pago.
     */
    @Transactional
    public AlumnoResponse registrarAlumno(RegistrarAlumnoRequest request) {

        String dniNormalizado = normalizarDni(request.getDni());
        String email = normalizarEmail(request.getEmail());
        LocalDate hoy = ZonaHoraria.hoy();

        int edad = Period.between(request.getFechaNacimiento(), hoy).getYears();
        if (edad < EDAD_MINIMA) {
            throw new RegistroInvalidoException(
                "El alumno debe tener al menos " + EDAD_MINIMA + " años para registrarse");
        }

        if (alumnoRepository.existsByDni(dniNormalizado)) {
            throw new RegistroInvalidoException("Ya existe un alumno registrado con ese DNI");
        }

        if (cuentaRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        Membresia membresia = buscarMembresiaOLanzarError(request.getMembresiaId());
        validarMedioDePago(request.getMedioDePago());

        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(email);
        cuenta.setPassword(passwordEncoder.encode(dniNormalizado));
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setDebeCambiarPassword(true);
        cuenta.setActiva(true);

        Alumno alumno = new Alumno();
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEdad(edad);
        alumno.setTelefono(request.getTelefono());
        alumno.setSexo(request.getSexo());
        alumno.setModalidad(request.getModalidad());
        alumno.setDni(dniNormalizado);
        alumno.setEmail(email);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.setCuenta(cuenta);
        Cuota primeraCuota = inscribirConPrimeraCuota(alumno, membresia, hoy);

        Alumno alumnoGuardado = alumnoRepository.save(alumno);
        pagoRepository.save(Pago.de(primeraCuota, SIN_RECARGO, request.getMedioDePago(), hoy));

        return mapearAResponse(alumnoGuardado);
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarAlumnos(boolean activos) {
        return alumnoRepository.findByCuentaActiva(activos)
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlumnoResponse obtenerAlumno(Long id) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        return mapearAResponse(alumno);
    }

    /** Datos del alumno dueño de la cuenta con ese email (el usuario logueado). */
    @Transactional(readOnly = true)
    public AlumnoResponse obtenerAlumnoPorEmail(String email) {
        Alumno alumno = alumnoRepository.findByCuentaEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un alumno asociado a esta cuenta"));
        return mapearAResponse(alumno);
    }

    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, ActualizarAlumnoRequest request) {
        Alumno alumno = buscarAlumnoOLanzarError(id);

        String dniNormalizado = normalizarDni(request.getDni());
        String email = normalizarEmail(request.getEmail());
        boolean dniCambio = !alumno.getDni().equals(dniNormalizado);

        if (dniCambio && alumnoRepository.existsByDniAndIdNot(dniNormalizado, id)) {
            throw new RegistroInvalidoException("Ya existe otro alumno registrado con ese DNI");
        }

        int edad = Period.between(request.getFechaNacimiento(), ZonaHoraria.hoy()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new RegistroInvalidoException(
                "El alumno debe tener al menos " + EDAD_MINIMA + " años");
        }

        String emailActual = alumno.getCuenta().getEmail();
        if (!emailActual.equalsIgnoreCase(email)
                && cuentaRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistroInvalidoException("Ya existe una cuenta registrada con ese email");
        }

        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setEdad(edad);
        alumno.setTelefono(request.getTelefono());
        alumno.setSexo(request.getSexo());
        alumno.setModalidad(request.getModalidad());
        alumno.setDni(dniNormalizado);
        alumno.setEmail(email);
        alumno.setFechaNacimiento(request.getFechaNacimiento());
        alumno.setObjetivo(request.getObjetivo());
        alumno.setCuestionarioSalud(request.getCuestionarioSalud());
        alumno.getCuenta().setEmail(email);

        // Si el alumno todavía no cambió la contraseña inicial (que es su DNI), pasa a ser el DNI nuevo.
        // Si ya la cambió, solo se actualiza el DNI.
        if (dniCambio && alumno.getCuenta().isDebeCambiarPassword()) {
            alumno.getCuenta().setPassword(passwordEncoder.encode(dniNormalizado));
        }

        Alumno alumnoActualizado = alumnoRepository.save(alumno);
        return mapearAResponse(alumnoActualizado);
    }

    /** Vista previa del cambio de membresía, para el aviso de confirmación del front. */
    @Transactional(readOnly = true)
    public CambioMembresiaResponse previsualizarCambioMembresia(Long id, Long membresiaId) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        Membresia nueva = buscarMembresiaOLanzarError(membresiaId);
        CambioMembresia cambio = evaluarCambioMembresia(alumno, nueva, ZonaHoraria.hoy());
        BigDecimal precioNuevo = precio(nueva);
        BigDecimal pagado = cambio.pagadoDelPeriodo();
        BigDecimal diferencia = cambio.diferencia();
        return new CambioMembresiaResponse(
            cambio.permitido(),
            cambio.motivo(),
            alumno.buscarInscripcionActiva().map(i -> i.getMembresia().getNombre()).orElse(null),
            nueva.getNombre(),
            ZonaHoraria.hoy(),
            cambio.cuotaVigente().map(Cuota::getFechaVencimiento).orElse(null),
            pagado,
            precioNuevo,
            diferencia,
            pagado != null && precioNuevo.compareTo(pagado) < 0,
            diferencia != null && diferencia.signum() > 0
        );
    }

    /**
     * Cambio de membresía (reglas M1 a M9). La inscripción activa se cancela con fecha de fin hoy y su cuota queda tal
     * cual se pagó; se crea una inscripción nueva con la membresía nueva desde hoy y una cuota desde hoy hasta el mismo
     * vencimiento, por la diferencia a cobrar, con su pago (fecha hoy). Si la nueva no es más cara, la cuota y el pago
     * son de $0 y el pago copia el medio del pago anterior (no se cobra nada, el medio es irrelevante).
     */
    @Transactional
    public AlumnoResponse cambiarMembresia(Long id, CambiarMembresiaRequest request) {
        LocalDate hoy = ZonaHoraria.hoy();
        Alumno alumno = buscarAlumnoOLanzarError(id);
        Membresia nueva = buscarMembresiaOLanzarError(request.getMembresiaId());
        CambioMembresia cambio = evaluarCambioMembresia(alumno, nueva, hoy);
        if (!cambio.permitido()) {
            throw new RegistroInvalidoException(cambio.motivo());
        }

        Optional<Cuota> vigente = cambio.cuotaVigente();
        MedioDePago medioDePago = vigente.isPresent() ? medioDePagoDelCambio(cambio, request) : null;

        alumno.buscarInscripcionActiva().ifPresent(i -> i.cancelar(hoy));
        Inscripcion inscripcionNueva = nuevaInscripcion(nueva, hoy);
        alumno.agregarInscripcion(inscripcionNueva);
        // Datos viejos sin cuotas: no hay período que completar, la próxima renovación empieza hoy
        vigente.ifPresent(c -> inscripcionNueva.agregarCuotaPorCambio(hoy, c.getFechaVencimiento(), cambio.diferencia()));

        Alumno guardado = alumnoRepository.save(alumno);
        if (vigente.isPresent()) {
            pagoRepository.save(Pago.de(cuotaGuardada(guardado), SIN_RECARGO, medioDePago, hoy));
        }
        return mapearAResponse(guardado);
    }

    /** Con diferencia a cobrar el medio es obligatorio; sin diferencia se copia el del pago anterior. */
    private MedioDePago medioDePagoDelCambio(CambioMembresia cambio, CambiarMembresiaRequest request) {
        if (cambio.diferencia().signum() > 0) {
            if (request.getMedioDePago() == null) {
                throw new RegistroInvalidoException("Seleccioná el medio con el que se paga la diferencia");
            }
            return request.getMedioDePago();
        }
        return pagoRepository.findFirstByCuotaIdOrderByIdAsc(cambio.cuotaVigente().get().getId())
                .map(Pago::getMedioDePago)
                // Datos viejos: la cuota anterior puede no tener pago
                .or(() -> Optional.ofNullable(request.getMedioDePago()))
                .orElseThrow(() -> new RegistroInvalidoException("Seleccioná el medio de pago"));
    }

    /**
     * Baja lógica (MOGNI-246): el alumno no se borra, su cuenta pasa a inactiva (no puede iniciar sesión), se cancela
     * su inscripción y sus turnos que todavía no empezaron. Si ya había pagado el mes siguiente, no se devuelve.
     */
    @Transactional
    public AlumnoResponse darDeBajaAlumno(Long id) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        bajaAlumnoService.darDeBaja(alumno, ZonaHoraria.ahora());
        return mapearAResponse(alumnoRepository.save(alumno));
    }

    /**
     * Reactivación (MOGNI-246). Si su última cuota todavía cubre hoy, se reactiva la misma inscripción sin cobrar
     * nada. Si no, es como un alta: se crean una inscripción nueva con la membresía elegida y su primera cuota pagada
     * (hoy → hoy + 30), sin recargo: la deuda anterior se olvida.
     */
    @Transactional
    public AlumnoResponse reactivarAlumno(Long id, ReactivarAlumnoRequest request) {
        Alumno alumno = buscarAlumnoOLanzarError(id);
        if (alumno.getCuenta().isActiva()) {
            return mapearAResponse(alumno);
        }
        LocalDate hoy = ZonaHoraria.hoy();
        Optional<Cuota> conDiasPagos = alumno.buscarUltimaCuota()
                .filter(cuota -> !hoy.isAfter(cuota.getFechaVencimiento()));

        if (conDiasPagos.isPresent()) {
            Inscripcion inscripcion = conDiasPagos.get().getInscripcion();
            cancelarInscripcionesActivas(alumno, inscripcion, hoy);
            inscripcion.reactivar();
            activarCuenta(alumno);
            return mapearAResponse(alumnoRepository.save(alumno));
        }

        Long membresiaId = request != null ? request.getMembresiaId() : null;
        MedioDePago medioDePago = request != null ? request.getMedioDePago() : null;
        if (membresiaId == null || medioDePago == null) {
            throw new RegistroInvalidoException(
                "Para reactivar al alumno elegí la membresía y el medio de pago de la primera cuota");
        }
        Membresia membresia = buscarMembresiaOLanzarError(membresiaId);
        cancelarInscripcionesActivas(alumno, null, hoy);
        inscribirConPrimeraCuota(alumno, membresia, hoy);
        activarCuenta(alumno);
        Alumno guardado = alumnoRepository.save(alumno);
        pagoRepository.save(Pago.de(cuotaGuardada(guardado), SIN_RECARGO, medioDePago, hoy));
        return mapearAResponse(guardado);
    }

    /**
     * La cuota recién creada, tal como quedó guardada. Con un alumno que ya existía, save() hace un merge: la
     * inscripción y la cuota nuevas que se guardan son copias de las que se armaron, así que el pago tiene que apuntar
     * a la copia guardada (si apunta al objeto original, Hibernate lo rechaza por no estar guardado). Es la última
     * cuota de la inscripción activa, que es la nueva.
     */
    private static Cuota cuotaGuardada(Alumno guardado) {
        return guardado.buscarUltimaCuotaActiva().orElseThrow();
    }

    private static void activarCuenta(Alumno alumno) {
        alumno.getCuenta().setActiva(true);
        alumno.getCuenta().setFechaBaja(null);
    }

    /** Datos viejos: alumnos dados de baja antes del rediseño pueden tener todavía una inscripción ACTIVA. */
    private static void cancelarInscripcionesActivas(Alumno alumno, Inscripcion excepto, LocalDate hoy) {
        alumno.getInscripciones().stream()
                .filter(i -> i != excepto && i.getEstado() == EstadoInscripcion.ACTIVA)
                .forEach(i -> i.cancelar(hoy));
    }

    private Alumno buscarAlumnoOLanzarError(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno con id " + id));
    }

    /**
     * Resultado de validar un cambio de membresía: lo usan la vista previa y el cambio, así nunca difieren.
     * pagadoDelPeriodo y diferencia son null si no hay cuota vigente (datos viejos) o si no se permite.
     */
    record CambioMembresia(boolean permitido, String motivo, Optional<Cuota> cuotaVigente,
                           BigDecimal pagadoDelPeriodo, BigDecimal diferencia) {

        static CambioMembresia rechazado(String motivo, Optional<Cuota> cuotaVigente) {
            return new CambioMembresia(false, motivo, cuotaVigente, null, null);
        }
    }

    /**
     * Lo pagado por el período vigente, sin recargo: la cuota vigente más las de cambios de membresía anteriores del
     * mismo período (todas terminan el mismo día). Así, si ya cambió una vez, no se le cobra dos veces lo mismo.
     */
    static BigDecimal pagadoDelPeriodo(Alumno alumno, Cuota vigente) {
        return alumno.getInscripciones().stream()
                .flatMap(i -> i.getCuotas().stream())
                .filter(c -> c.getFechaVencimiento().equals(vigente.getFechaVencimiento()))
                .map(Cuota::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }

    /** Reglas M1, M2 y M9 del rediseño de cuotas, y la diferencia a cobrar (M5). */
    static CambioMembresia evaluarCambioMembresia(Alumno alumno, Membresia nueva, LocalDate hoy) {
        Optional<Cuota> cuota = alumno.buscarUltimaCuotaActiva();
        if (!alumno.getCuenta().isActiva()) {
            return CambioMembresia.rechazado(
                "No se puede cambiar la membresía de un alumno inactivo: se elige al reactivarlo", cuota);
        }
        boolean mismaMembresia = alumno.buscarInscripcionActiva()
                .map(i -> Objects.equals(i.getMembresia().getId(), nueva.getId()))
                .orElse(false);
        if (mismaMembresia) {
            return CambioMembresia.rechazado("Elegí una membresía distinta a la actual", cuota);
        }
        if (cuota.isEmpty()) {
            // Solo con datos viejos: no hay período que completar ni nada que cobrar
            return new CambioMembresia(true, null, cuota, null, null);
        }
        SituacionPago situacion = SituacionPago.de(cuota.get(), hoy);
        if (situacion == SituacionPago.VENCIDA || situacion == SituacionPago.EN_MORA) {
            return CambioMembresia.rechazado("No se puede cambiar la membresía: la cuota está "
                    + (situacion == SituacionPago.VENCIDA ? "vencida" : "en mora")
                    + ". Primero hay que registrar el pago", cuota);
        }
        LocalDate inicio = cuota.get().getFechaInicio();
        if (inicio != null && inicio.isAfter(hoy)) {
            return CambioMembresia.rechazado("No se puede cambiar la membresía: ya pagó el período que empieza el "
                    + inicio.format(FECHA) + ". Se va a poder cambiar desde ese día", cuota);
        }
        BigDecimal pagado = pagadoDelPeriodo(alumno, cuota.get());
        return new CambioMembresia(true, null, cuota, pagado, ReglasCuota.diferenciaCambio(pagado, precio(nueva)));
    }

    /** Inscripción nueva desde hoy con su primera cuota (hoy → hoy + 30). El pago lo guarda quien la llama. */
    private static Cuota inscribirConPrimeraCuota(Alumno alumno, Membresia membresia, LocalDate hoy) {
        Inscripcion inscripcion = nuevaInscripcion(membresia, hoy);
        alumno.agregarInscripcion(inscripcion);
        return inscripcion.agregarCuota(hoy);
    }

    private static Inscripcion nuevaInscripcion(Membresia membresia, LocalDate hoy) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setFechaInicio(hoy);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setMembresia(membresia);
        return inscripcion;
    }

    private static BigDecimal precio(Membresia membresia) {
        return BigDecimal.valueOf(membresia.getPrecioActual()).setScale(2);
    }

    private static void validarMedioDePago(MedioDePago medioDePago) {
        if (medioDePago == null) {
            throw new RegistroInvalidoException("Debe seleccionarse el medio de pago de la primera cuota");
        }
    }

    private String normalizarDni(String dni) {
        String dniNormalizado = dni.replaceAll("[^0-9]", "");
        if (dniNormalizado.isEmpty()) {
            throw new RegistroInvalidoException("El DNI debe contener números");
        }
        return dniNormalizado;
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Membresia buscarMembresiaOLanzarError(Long membresiaId) {
        if (membresiaId == null) {
            throw new RegistroInvalidoException("Debe asignarse una membresía");
        }
        return membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new RegistroInvalidoException("La membresía indicada no existe"));
    }

    private AlumnoResponse mapearAResponse(Alumno alumno) {
        Optional<Inscripcion> activa = alumno.buscarInscripcionActiva();
        Membresia membresia = activa.map(Inscripcion::getMembresia).orElse(null);
        boolean cuentaActiva = alumno.getCuenta().isActiva();
        // Solo para inactivos: así el listado de activos no carga las cuotas de cada alumno
        LocalDate vencimientoUltimaCuota = cuentaActiva ? null
                : alumno.buscarUltimaCuota().map(Cuota::getFechaVencimiento).orElse(null);
        Boolean reactivacionRequierePago = cuentaActiva ? null
                : vencimientoUltimaCuota == null || ZonaHoraria.hoy().isAfter(vencimientoUltimaCuota);
        return new AlumnoResponse(
            alumno.getId(),
            alumno.getNombre(),
            alumno.getApellido(),
            alumno.getEdad(),
            alumno.getTelefono(),
            alumno.getSexo(),
            alumno.getModalidad(),
            alumno.getDni(),
            alumno.getEmail(),
            alumno.getFechaNacimiento(),
            alumno.getObjetivo(),
            alumno.getCuestionarioSalud(),
            alumno.getCuenta().getRol(),
            cuentaActiva,
            alumno.getCuenta().getFechaBaja(),
            membresia != null ? membresia.getId() : null,
            membresia != null ? membresia.getNombre() : null,
            activa.map(Inscripcion::getFechaInicio).orElse(null),
            alumno.buscarInscripcionActual().map(this::mapearInscripcion).orElse(null),
            vencimientoUltimaCuota,
            reactivacionRequierePago
        );
    }

    private InscripcionResponse mapearInscripcion(Inscripcion inscripcion) {
        return new InscripcionResponse(
            inscripcion.getMembresia().getId(),
            inscripcion.getMembresia().getNombre(),
            inscripcion.getEstado(),
            inscripcion.getFechaInicio(),
            inscripcion.getFechaFin()
        );
    }
}
