package mogni.system_backend.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.panel.dto.PlanesPorVencerResponse;
import mogni.system_backend.panel.service.PlanesPorVencerService;
import mogni.system_backend.plan.entity.MetodoPlan;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/**
 * Planes por vencer de Inicio, contra un PostgreSQL embebido (nunca la base compartida) y con un "hoy" fijo. Con su
 * propio contexto y su propia base, así vaciar las tablas no afecta a otras pruebas.
 */
@SpringBootTest(properties = "mogni.test.contexto=planes-por-vencer")
@ActiveProfiles("test")
class PlanesPorVencerIntegrationTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);
    private static final AtomicInteger SECUENCIA = new AtomicInteger(7000);

    @Autowired
    private PlanesPorVencerService service;

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private PlanAsignadoRepository planRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciarBase() {
        jdbc.execute("TRUNCATE TABLE alumnos, cuentas RESTART IDENTITY CASCADE");
    }

    private Alumno alumno(String apellido, boolean activo) {
        int n = SECUENCIA.incrementAndGet();
        Cuenta cuenta = new Cuenta();
        cuenta.setEmail("planes" + n + "@test.com");
        cuenta.setPassword("x");
        cuenta.setRol(Rol.ALUMNO);
        cuenta.setActiva(activo);
        Alumno alumno = new Alumno();
        alumno.setNombre("Alumno");
        alumno.setApellido(apellido);
        alumno.setEdad(30);
        alumno.setDni(String.valueOf(43_000_000 + n));
        alumno.setEmail(cuenta.getEmail());
        alumno.setCuenta(cuenta);
        return alumnoRepository.save(alumno);
    }

    /** Plan ACTIVO asignado hace un mes que vence (fecha fin inclusiva) {@code diasHastaFin} días después de hoy. */
    private PlanAsignado plan(Alumno alumno, int diasHastaFin) {
        PlanAsignado plan = new PlanAsignado(alumno, HOY.minusMonths(1));
        plan.setNombre("Plan de " + alumno.getApellido());
        plan.setNivel(NivelPlantilla.INTERMEDIO);
        plan.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        plan.setMetodo(MetodoPlan.ULTRA_MASS);
        plan.setMesociclo("Hipertrofia");
        plan.setFechaFin(HOY.plusDays(diasHastaFin));
        return planRepository.save(plan);
    }

    private static List<String> apellidos(PlanesPorVencerResponse respuesta) {
        return respuesta.planes().stream().map(PlanesPorVencerResponse.PlanPorVencer::apellidoAlumno).toList();
    }

    @Test
    void venceEnTresDiasApareceYEnOchoNo() {
        plan(alumno("Tres", true), 3);
        plan(alumno("Ocho", true), 8);

        PlanesPorVencerResponse respuesta = service.consultar(HOY);

        assertEquals(List.of("Tres"), apellidos(respuesta));
        PlanesPorVencerResponse.PlanPorVencer tres = respuesta.planes().get(0);
        assertEquals(3, tres.diasParaVencer());
        assertEquals(HOY.plusDays(3), tres.fechaFin());
        assertEquals("Plan de Tres", tres.nombre());
    }

    @Test
    void cubreDeHoyALosSeisDiasSiguientesOrdenadosPorFechaFin() {
        plan(alumno("Seis", true), 6);   // último día del aviso
        plan(alumno("Hoy", true), 0);    // vence hoy: todavía vigente
        plan(alumno("Siete", true), 7);  // ya es el octavo día
        plan(alumno("Dos", true), 2);

        PlanesPorVencerResponse respuesta = service.consultar(HOY);

        assertEquals(PlanesPorVencerService.DIAS_AVISO_VENCIMIENTO_PLAN, respuesta.diasAviso());
        assertEquals(List.of("Hoy", "Dos", "Seis"), apellidos(respuesta));
        assertEquals(0, respuesta.planes().get(0).diasParaVencer());
    }

    @Test
    void unPlanQueYaVencioNoAparece() {
        plan(alumno("Ayer", true), -1);  // venció ayer: ya no es vigente

        assertTrue(service.consultar(HOY).planes().isEmpty());
    }

    @Test
    void elPlanDeUnAlumnoInactivoNoAparece() {
        plan(alumno("Inactivo", false), 2);

        assertTrue(service.consultar(HOY).planes().isEmpty());
    }
}
