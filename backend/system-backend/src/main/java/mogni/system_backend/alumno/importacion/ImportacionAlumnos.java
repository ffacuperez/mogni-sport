package mogni.system_backend.alumno.importacion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.importacion.PlanillaAlumnos.Fila;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.repository.CuentaRepository;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.ReglasCuota;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.membresia.repository.MembresiaRepository;

/**
 * Carga de una sola vez los alumnos que ya entrenan en el gimnasio (ver PlanillaAlumnos). Solo corre con el perfil
 * "importar": revisa la planilla, muestra qué cargaría y termina. Con app.importacion.confirmar=true, además guarda
 * todo junto (o nada, si algo falla).
 * <p>
 * Cada alumno queda como en un alta: cuenta con el email y el DNI como contraseña (la cambia al entrar), inscripción
 * activa y una cuota. Diferencias con el alta: la cuota empieza en su último día de pago (no hoy), no se registra
 * ningún pago (pagaron antes de usar el sistema: no es ingreso de hoy) y, sin fecha de nacimiento, queda con 99 años.
 */
@Component
@Profile("importar")
public class ImportacionAlumnos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportacionAlumnos.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final int EDAD_SIN_FECHA = 99;

    private final AlumnoRepository alumnoRepository;
    private final CuentaRepository cuentaRepository;
    private final MembresiaRepository membresiaRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaccion;
    private final ConfigurableApplicationContext contexto;
    private final String archivo;
    private final boolean confirmar;

    public ImportacionAlumnos(AlumnoRepository alumnoRepository, CuentaRepository cuentaRepository,
                              MembresiaRepository membresiaRepository, PasswordEncoder passwordEncoder,
                              PlatformTransactionManager transactionManager, ConfigurableApplicationContext contexto,
                              @Value("${app.importacion.archivo}") String archivo,
                              @Value("${app.importacion.confirmar:false}") boolean confirmar) {
        this.alumnoRepository = alumnoRepository;
        this.cuentaRepository = cuentaRepository;
        this.membresiaRepository = membresiaRepository;
        this.passwordEncoder = passwordEncoder;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.contexto = contexto;
        this.archivo = archivo;
        this.confirmar = confirmar;
    }

    private record Alta(Fila fila, Membresia membresia) {
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        PlanillaAlumnos.Resultado planilla =
                PlanillaAlumnos.leer(Files.readAllLines(Path.of(archivo), StandardCharsets.UTF_8));
        List<String> errores = new ArrayList<>(planilla.errores());
        List<Membresia> membresias = membresiaRepository.findAll();
        List<Alta> altas = new ArrayList<>();
        for (Fila fila : planilla.filas()) {
            List<Membresia> candidatas = membresias.stream()
                    .filter(membresia -> membresia.getFrecuenciaSemanal() == fila.diasPorSemana()).toList();
            if (candidatas.size() != 1) {
                errores.add("Línea " + fila.linea() + ": hay " + candidatas.size() + " membresías de "
                        + fila.diasPorSemana() + " días por semana y tiene que haber una");
            } else if (alumnoRepository.existsByDni(fila.dni())) {
                errores.add("Línea " + fila.linea() + ": ya hay un alumno con el DNI " + fila.dni());
            } else if (cuentaRepository.existsByEmailIgnoreCase(fila.email())) {
                errores.add("Línea " + fila.linea() + ": ya hay una cuenta con el email " + fila.email());
            } else {
                altas.add(new Alta(fila, candidatas.get(0)));
            }
        }

        LocalDate hoy = ZonaHoraria.hoy();
        for (Alta alta : altas) {
            LocalDate inicio = PlanillaAlumnos.ultimoDiaDePago(alta.fila().diaDePago(), hoy);
            log.info("Línea {}: {}, {} (DNI {}) - {} - cuota del {} al {}", alta.fila().linea(), alta.fila().apellido(),
                    alta.fila().nombre(), alta.fila().dni(), alta.membresia().getNombre(), inicio.format(FECHA),
                    ReglasCuota.vencimientoDesde(inicio).format(FECHA));
        }
        if (!errores.isEmpty()) {
            errores.forEach(log::error);
            log.error("La planilla tiene {} errores: no se guardó nada", errores.size());
            terminar(1);
            return;
        }
        if (!confirmar) {
            log.info("Planilla revisada: {} alumnos listos. No se guardó nada; para guardarlos, correr con "
                    + "APP_IMPORTACION_CONFIRMAR=true", altas.size());
            terminar(0);
            return;
        }
        transaccion.executeWithoutResult(estado -> altas.forEach(alta -> guardar(alta, hoy)));
        log.info("Se importaron {} alumnos", altas.size());
        terminar(0);
    }

    private void guardar(Alta alta, LocalDate hoy) {
        Fila fila = alta.fila();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail(fila.email());
        cuenta.setPassword(passwordEncoder.encode(fila.dni()));
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setDebeCambiarPassword(true);
        cuenta.setActiva(true);

        Alumno alumno = new Alumno();
        alumno.setNombre(fila.nombre());
        alumno.setApellido(fila.apellido());
        alumno.setDni(fila.dni());
        alumno.setEmail(fila.email());
        alumno.setSexo(fila.sexo());
        alumno.setEdad(EDAD_SIN_FECHA);
        alumno.setFechaNacimiento(hoy.minusYears(EDAD_SIN_FECHA));
        alumno.setCuenta(cuenta);

        LocalDate inicio = PlanillaAlumnos.ultimoDiaDePago(fila.diaDePago(), hoy);
        Inscripcion.nueva(alumno, alta.membresia(), inicio, EstadoInscripcion.ACTIVA).agregarCuota(inicio);
        alumnoRepository.save(alumno);
    }

    private void terminar(int codigo) {
        System.exit(SpringApplication.exit(contexto, () -> codigo));
    }
}
