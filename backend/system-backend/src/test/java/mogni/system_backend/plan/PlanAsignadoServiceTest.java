package mogni.system_backend.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.alumno.repository.AlumnoRepository;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plan.dto.AsignarPlanDesdeCeroRequest;
import mogni.system_backend.plan.dto.AsignarPlanDesdePlantillaRequest;
import mogni.system_backend.plan.dto.PlanAsignadoResponse;
import mogni.system_backend.plan.dto.PlanRequest;
import mogni.system_backend.plan.entity.EjercicioSesionPlan;
import mogni.system_backend.plan.entity.EstadoPlanAsignado;
import mogni.system_backend.plan.entity.PlanAsignado;
import mogni.system_backend.plan.repository.PlanAsignadoRepository;
import mogni.system_backend.plan.service.PlanAsignadoService;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.EjercicioSesion;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.entity.SesionPlantilla;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;

/** HU-266: pruebas de aceptación de "Asignar plan de entrenamiento a alumno" a nivel servicio. */
@ExtendWith(MockitoExtension.class)
class PlanAsignadoServiceTest {

    @Mock
    private PlanAsignadoRepository planRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private PlantillaPlanRepository plantillaRepository;

    @Mock
    private EjercicioRepository ejercicioRepository;

    @InjectMocks
    private PlanAsignadoService planService;

    private final Map<Long, Ejercicio> catalogo = Map.of(
            1L, new Ejercicio(1L, "Press de banca con barra", "Pecho", null, true),
            2L, new Ejercicio(2L, "Remo con mancuerna", "Espalda", null, true),
            9L, new Ejercicio(9L, "Peso muerto rumano", "Piernas", null, false));

    private Alumno juan;

    @BeforeEach
    void setUp() {
        juan = alumno(7L, "Juan", "Pérez", true);
        lenient().when(alumnoRepository.findByIdConBloqueo(7L)).thenReturn(Optional.of(juan));
        lenient().when(planRepository.findFirstByAlumnoIdAndEstado(anyLong(), any())).thenReturn(Optional.empty());
        lenient().when(planRepository.save(any(PlanAsignado.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ejercicioRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return ids.stream().filter(catalogo::containsKey).map(catalogo::get).toList();
        });
    }

    // --- Datos de prueba ---

    private static Alumno alumno(Long id, String nombre, String apellido, boolean activo) {
        Alumno a = new Alumno();
        a.setId(id);
        a.setNombre(nombre);
        a.setApellido(apellido);
        Cuenta cuenta = new Cuenta();
        cuenta.setActiva(activo);
        a.setCuenta(cuenta);
        return a;
    }

    /** Plantilla con dos sesiones: press de banca 4x8-10 y remo 3x12. */
    private PlantillaPlan plantilla(Long id) {
        PlantillaPlan p = new PlantillaPlan();
        p.setId(id);
        p.setNombre("Hipertrofia - Buenardo");
        p.setNivel(NivelPlantilla.INTERMEDIO);
        p.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        p.setFechaActualizacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        SesionPlantilla empuje = new SesionPlantilla("Pecho");
        empuje.agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(1L), 4, 8, 10, new BigDecimal("2"),
                new BigDecimal("60"), 2)));
        SesionPlantilla tiron = new SesionPlantilla(null);
        tiron.agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(2L), 3, 12, null, null, null, null)));
        p.reemplazarSesiones(new ArrayList<>(List.of(empuje, tiron)));
        return p;
    }

    private static EjercicioSesionRequest ejercicio(Long id, int series, int repeticiones) {
        EjercicioSesionRequest e = new EjercicioSesionRequest();
        e.setEjercicioId(id);
        e.setSeries(series);
        e.setRepeticiones(repeticiones);
        return e;
    }

    private static SesionRequest sesion(String nombre, EjercicioSesionRequest... ejercicios) {
        SesionRequest s = new SesionRequest();
        s.setNombre(nombre);
        s.setEjercicios(new ArrayList<>(List.of(ejercicios)));
        return s;
    }

    private static AsignarPlanDesdeCeroRequest desdeCero(Long alumnoId, SesionRequest... sesiones) {
        AsignarPlanDesdeCeroRequest r = new AsignarPlanDesdeCeroRequest();
        r.setAlumnoId(alumnoId);
        r.setNombre("  Plan de Juan  ");
        r.setNivel(NivelPlantilla.PRINCIPIANTE);
        r.setObjetivo(ObjetivoPlantilla.FUERZA);
        r.setSesiones(new ArrayList<>(List.of(sesiones)));
        r.setCantidadSemanas(SEMANAS);
        return r;
    }

    private static AsignarPlanDesdePlantillaRequest desdePlantilla(Long alumnoId, Long plantillaId) {
        AsignarPlanDesdePlantillaRequest r = new AsignarPlanDesdePlantillaRequest();
        r.setAlumnoId(alumnoId);
        r.setPlantillaId(plantillaId);
        r.setCantidadSemanas(SEMANAS);
        return r;
    }

    /** HU-266: la vigencia en semanas es obligatoria. Los tests que no tratan de la vigencia usan esta. */
    private static final int SEMANAS = 8;

    private PlanAsignado planGuardado() {
        ArgumentCaptor<PlanAsignado> captor = ArgumentCaptor.forClass(PlanAsignado.class);
        verify(planRepository).save(captor.capture());
        return captor.getValue();
    }

    private PlanAsignado planActivoDe(Alumno alumno, String nombre) {
        PlanAsignado plan = new PlanAsignado(alumno, LocalDate.of(2026, 10, 12));
        plan.setId(30L);
        plan.setNombre(nombre);
        plan.setNivel(NivelPlantilla.INTERMEDIO);
        plan.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        return plan;
    }

    // --- Desde una plantilla ---

    @Test
    void asignacion_de_una_plantilla_a_un_alumno_sin_plan_activo_pasa_con_una_copia_independiente() {
        PlantillaPlan plantilla = plantilla(5L);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantilla));

        PlanAsignadoResponse asignado = planService.asignarDesdePlantilla(desdePlantilla(7L, 5L));

        assertEquals("Hipertrofia - Buenardo", asignado.getNombre());
        assertEquals(NivelPlantilla.INTERMEDIO, asignado.getNivel());
        assertEquals(ObjetivoPlantilla.HIPERTROFIA, asignado.getObjetivo());
        assertEquals(EstadoPlanAsignado.ACTIVO, asignado.getEstado());
        assertEquals(7L, asignado.getAlumnoId());
        assertEquals("Juan Pérez", asignado.getAlumnoNombre());
        assertEquals(5L, asignado.getPlantillaOrigenId());
        assertEquals(2, asignado.getSesiones().size());
        var press = asignado.getSesiones().get(0).getEjercicios().get(0);
        assertEquals("Pecho", asignado.getSesiones().get(0).getNombre());
        assertEquals(4, press.getSeries());
        assertEquals(8, press.getRepeticiones());
        assertEquals(10, press.getRepeticionesMax());
        assertEquals(new BigDecimal("2"), press.getDescansoMinutos());
        assertEquals(new BigDecimal("60"), press.getPesoSugerido());
        assertEquals(2, press.getRirObjetivo());
        // Copia profunda: sesiones y ejercicios son objetos nuevos, no los de la plantilla
        PlanAsignado plan = planGuardado();
        assertNotSame(plantilla.getSesiones().get(0), plan.getSesiones().get(0));
        assertNotSame(plantilla.getSesiones().get(0).getEjercicios().get(0), plan.getSesiones().get(0).getEjercicios().get(0));
        // La plantilla suma una asignación activa
        verify(plantillaRepository).sumarAsignacion(5L);
    }

    @Test
    void modificacion_de_la_plantilla_original_luego_de_asignada_no_cambia_la_copia() {
        PlantillaPlan plantilla = plantilla(5L);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantilla));
        planService.asignarDesdePlantilla(desdePlantilla(7L, 5L));
        PlanAsignado plan = planGuardado();

        plantilla.setNombre("Otro nombre");
        plantilla.setNivel(NivelPlantilla.AVANZADO);
        plantilla.getSesiones().get(0).setNombre("Cambiada");
        plantilla.getSesiones().get(0).getEjercicios().get(0).setSeries(10);
        plantilla.reemplazarSesiones(new ArrayList<>(List.of(plantilla.getSesiones().get(1))));

        assertEquals("Hipertrofia - Buenardo", plan.getNombre());
        assertEquals(NivelPlantilla.INTERMEDIO, plan.getNivel());
        assertEquals(2, plan.getSesiones().size());
        assertEquals("Pecho", plan.getSesiones().get(0).getNombre());
        assertEquals(4, plan.getSesiones().get(0).getEjercicios().get(0).getSeries());
    }

    @Test
    void modificacion_de_la_copia_asignada_no_cambia_la_plantilla_original() {
        PlantillaPlan plantilla = plantilla(5L);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantilla));
        planService.asignarDesdePlantilla(desdePlantilla(7L, 5L));
        PlanAsignado plan = planGuardado();
        when(planRepository.findById(30L)).thenReturn(Optional.of(plan));

        PlanRequest cambios = desdeCero(7L, sesion("Solo remo", ejercicio(2L, 5, 5)));
        PlanAsignadoResponse modificado = planService.actualizarPlan(30L, cambios);

        assertEquals("Plan de Juan", modificado.getNombre());
        assertEquals(1, modificado.getSesiones().size());
        assertEquals("Hipertrofia - Buenardo", plantilla.getNombre());
        assertEquals(NivelPlantilla.INTERMEDIO, plantilla.getNivel());
        assertEquals(2, plantilla.getSesiones().size());
        assertEquals("Pecho", plantilla.getSesiones().get(0).getNombre());
        assertEquals(4, plantilla.getSesiones().get(0).getEjercicios().get(0).getSeries());
    }

    @Test
    void asignar_una_plantilla_inexistente_o_dada_de_baja_da_404() {
        when(plantillaRepository.findByIdAndActivaTrue(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> planService.asignarDesdePlantilla(desdePlantilla(7L, 99L)));
        verify(planRepository, never()).save(any());
        verify(plantillaRepository, never()).sumarAsignacion(anyLong());
    }

    // --- Desde cero ---

    @Test
    void asignacion_de_un_plan_armado_desde_cero_con_una_sesion_y_un_ejercicio_pasa() {
        PlanAsignadoResponse asignado = planService.asignarDesdeCero(desdeCero(7L, sesion(" Full body ", ejercicio(1L, 4, 10))));

        assertEquals("Plan de Juan", asignado.getNombre());
        assertEquals(NivelPlantilla.PRINCIPIANTE, asignado.getNivel());
        assertEquals(ObjetivoPlantilla.FUERZA, asignado.getObjetivo());
        assertEquals(EstadoPlanAsignado.ACTIVO, asignado.getEstado());
        assertNull(asignado.getPlantillaOrigenId());
        assertEquals(1, asignado.getSesiones().get(0).getNumero());
        assertEquals("Full body", asignado.getSesiones().get(0).getNombre());
        assertEquals("Press de banca con barra", asignado.getSesiones().get(0).getEjercicios().get(0).getNombre());
        // No se guarda como plantilla reutilizable
        verify(plantillaRepository, never()).save(any());
        verify(plantillaRepository, never()).sumarAsignacion(anyLong());
    }

    @Test
    void asignacion_de_un_plan_armado_desde_cero_sin_ninguna_sesion_falla() {
        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCero(7L)));

        assertEquals("El plan tiene que tener al menos una sesión", error.getMessage());
        verify(planRepository, never()).save(any());
    }

    @Test
    void plan_desde_cero_con_una_sesion_sin_ejercicios_falla() {
        assertThrows(RegistroInvalidoException.class, () -> planService.asignarDesdeCero(desdeCero(7L, sesion("Vacía"))));
    }

    @Test
    void plan_desde_cero_con_un_ejercicio_dado_de_baja_falla() {
        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCero(7L, sesion(null, ejercicio(9L, 3, 8)))));

        assertTrue(error.getMessage().contains("dado de baja"));
    }

    // --- Un plan activo a la vez ---

    @Test
    void intento_de_asignar_un_plan_a_un_alumno_con_plan_activo_falla_y_pide_finalizar_el_actual() {
        when(planRepository.findFirstByAlumnoIdAndEstado(7L, EstadoPlanAsignado.ACTIVO))
                .thenReturn(Optional.of(planActivoDe(juan, "Hipertrofia - Buenardo")));

        ConflictoException desdePlantilla = assertThrows(ConflictoException.class,
                () -> planService.asignarDesdePlantilla(desdePlantilla(7L, 5L)));
        ConflictoException desdeCero = assertThrows(ConflictoException.class,
                () -> planService.asignarDesdeCero(desdeCero(7L, sesion(null, ejercicio(1L, 4, 10)))));

        assertEquals("Juan ya tiene un plan activo (\"Hipertrofia - Buenardo\"). Para asignarle uno nuevo, "
                + "primero finalizá su plan actual", desdePlantilla.getMessage());
        assertEquals(desdePlantilla.getMessage(), desdeCero.getMessage());
        verify(planRepository, never()).save(any());
        verify(plantillaRepository, never()).sumarAsignacion(anyLong());
    }

    @Test
    void la_validacion_bloquea_al_alumno_para_que_dos_asignaciones_simultaneas_no_dejen_dos_planes_activos() {
        planService.asignarDesdeCero(desdeCero(7L, sesion(null, ejercicio(1L, 4, 10))));

        verify(alumnoRepository).findByIdConBloqueo(7L);
    }

    @Test
    void asignar_a_un_alumno_inexistente_da_404() {
        when(alumnoRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> planService.asignarDesdePlantilla(desdePlantilla(99L, 5L)));
    }

    @Test
    void asignar_a_un_alumno_dado_de_baja_falla() {
        when(alumnoRepository.findByIdConBloqueo(8L)).thenReturn(Optional.of(alumno(8L, "Ana", "Gómez", false)));

        assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCero(8L, sesion(null, ejercicio(1L, 4, 10)))));
    }

    @Test
    void finalizar_el_plan_activo_permite_asignar_uno_nuevo_y_libera_la_plantilla() {
        PlanAsignado activo = planActivoDe(juan, "Hipertrofia - Buenardo");
        activo.setPlantillaOrigen(plantilla(5L));
        when(planRepository.findById(30L)).thenReturn(Optional.of(activo));

        PlanAsignadoResponse finalizado = planService.finalizarPlan(30L);

        assertEquals(EstadoPlanAsignado.FINALIZADO, finalizado.getEstado());
        assertEquals(EstadoPlanAsignado.FINALIZADO, activo.getEstado());
        verify(plantillaRepository).restarAsignacion(5L);
    }

    @Test
    void finalizar_un_plan_desde_cero_no_toca_ninguna_plantilla() {
        when(planRepository.findById(30L)).thenReturn(Optional.of(planActivoDe(juan, "Plan de Juan")));

        planService.finalizarPlan(30L);

        verify(plantillaRepository, never()).restarAsignacion(anyLong());
    }

    @Test
    void finalizar_el_plan_activo_de_un_alumno_lo_finaliza_y_libera_la_plantilla() {
        PlanAsignado activo = planActivoDe(juan, "Hipertrofia - Buenardo");
        activo.setPlantillaOrigen(plantilla(5L));
        when(planRepository.findFirstByAlumnoIdAndEstado(7L, EstadoPlanAsignado.ACTIVO)).thenReturn(Optional.of(activo));

        planService.finalizarPlanActivoDe(7L);

        assertEquals(EstadoPlanAsignado.FINALIZADO, activo.getEstado());
        verify(plantillaRepository).restarAsignacion(5L);
    }

    @Test
    void finalizar_el_plan_activo_de_un_alumno_sin_plan_no_hace_nada() {
        planService.finalizarPlanActivoDe(7L);

        verify(planRepository, never()).save(any());
        verify(plantillaRepository, never()).restarAsignacion(anyLong());
    }

    @Test
    void finalizar_un_plan_ya_finalizado_falla() {
        PlanAsignado plan = planActivoDe(juan, "Plan de Juan");
        plan.finalizar(LocalDate.of(2026, 10, 20));
        when(planRepository.findById(30L)).thenReturn(Optional.of(plan));

        assertThrows(ConflictoException.class, () -> planService.finalizarPlan(30L));
    }

    @Test
    void un_plan_finalizado_no_se_puede_modificar() {
        PlanAsignado plan = planActivoDe(juan, "Plan de Juan");
        plan.finalizar(LocalDate.of(2026, 10, 20));
        when(planRepository.findById(30L)).thenReturn(Optional.of(plan));

        assertThrows(ConflictoException.class,
                () -> planService.actualizarPlan(30L, desdeCero(7L, sesion(null, ejercicio(1L, 4, 10)))));
    }

    @Test
    void la_modificacion_puede_conservar_un_ejercicio_que_se_dio_de_baja_despues_de_asignarlo() {
        PlanAsignado plan = planActivoDe(juan, "Plan de Juan");
        var sesion = new mogni.system_backend.plan.entity.SesionPlan(null);
        sesion.agregarEjercicios(List.of(new EjercicioSesionPlan(catalogo.get(9L), 3, 8, null, null, null, null)));
        plan.reemplazarSesiones(new ArrayList<>(List.of(sesion)));
        when(planRepository.findById(30L)).thenReturn(Optional.of(plan));

        PlanAsignadoResponse modificado = planService.actualizarPlan(30L, desdeCero(7L, sesion(null, ejercicio(9L, 4, 8))));

        assertEquals(4, modificado.getSesiones().get(0).getEjercicios().get(0).getSeries());
    }

    // --- Consulta del plan activo ---

    @Test
    void consultar_el_plan_activo_de_un_alumno_sin_plan_devuelve_vacio() {
        when(alumnoRepository.existsById(7L)).thenReturn(true);

        assertTrue(planService.obtenerPlanActivo(7L).isEmpty());
    }

    @Test
    void consultar_el_plan_activo_de_un_alumno_con_plan_lo_devuelve() {
        when(alumnoRepository.existsById(7L)).thenReturn(true);
        when(planRepository.findFirstByAlumnoIdAndEstado(7L, EstadoPlanAsignado.ACTIVO))
                .thenReturn(Optional.of(planActivoDe(juan, "Hipertrofia - Buenardo")));

        PlanAsignadoResponse activo = planService.obtenerPlanActivo(7L).orElseThrow();

        assertEquals("Hipertrofia - Buenardo", activo.getNombre());
        assertEquals(LocalDate.of(2026, 10, 12), activo.getFechaAsignacion());
    }

    @Test
    void consultar_el_plan_activo_de_un_alumno_inexistente_da_404() {
        when(alumnoRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> planService.obtenerPlanActivo(99L));
    }

    // --- HU-266: fecha fin y vigencia (fecha fija: martes 03/11/2026) ---

    private static final LocalDate HOY = LocalDate.of(2026, 11, 3);

    /** Plan ACTIVO en la base, asignado hace un mes y con la fecha fin indicada (null = plan viejo, sin fecha fin). */
    private PlanAsignado planActivoConFin(LocalDate fechaFin) {
        PlanAsignado plan = new PlanAsignado(juan, HOY.minusMonths(1));
        plan.setId(30L);
        plan.setNombre("Hipertrofia - Buenardo");
        plan.setNivel(NivelPlantilla.INTERMEDIO);
        plan.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        plan.setFechaFin(fechaFin);
        return plan;
    }

    private void tieneEnLaBase(PlanAsignado plan) {
        lenient().when(planRepository.findFirstByAlumnoIdAndEstado(7L, EstadoPlanAsignado.ACTIVO))
                .thenReturn(Optional.of(plan));
        lenient().when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        lenient().when(alumnoRepository.existsById(7L)).thenReturn(true);
    }

    private AsignarPlanDesdeCeroRequest desdeCeroPorSemanas(Integer semanas) {
        AsignarPlanDesdeCeroRequest r = desdeCero(7L, sesion(null, ejercicio(1L, 4, 10)));
        r.setCantidadSemanas(semanas);
        return r;
    }

    private AsignarPlanDesdePlantillaRequest desdePlantillaPorSemanas(Integer semanas) {
        AsignarPlanDesdePlantillaRequest r = desdePlantilla(7L, 5L);
        r.setCantidadSemanas(semanas);
        return r;
    }

    @Test
    void asignar_desde_plantilla_por_semanas_pasa_y_la_fecha_fin_es_el_ultimo_dia_de_la_ultima_semana() {
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantilla(5L)));

        PlanAsignadoResponse asignado = planService.asignarDesdePlantilla(desdePlantillaPorSemanas(3), HOY);

        // Martes 03/11 + 3 semanas: termina el lunes 23/11 inclusive
        assertEquals(HOY, asignado.getFechaAsignacion());
        assertEquals(LocalDate.of(2026, 11, 23), asignado.getFechaFin());
        assertEquals(3, asignado.getCantidadSemanas());
        assertEquals(EstadoPlanAsignado.ACTIVO, asignado.getEstado());
        assertEquals(LocalDate.of(2026, 11, 23), planGuardado().getFechaFin());
    }

    @Test
    void asignar_desde_cero_por_una_semana_pasa_y_termina_seis_dias_despues() {
        PlanAsignadoResponse asignado = planService.asignarDesdeCero(desdeCeroPorSemanas(1), HOY);

        assertEquals(HOY, asignado.getFechaAsignacion());
        assertEquals(HOY.plusDays(6), asignado.getFechaFin());
        assertEquals(1, asignado.getCantidadSemanas());
    }

    @Test
    void asignar_un_plan_sin_semanas_falla() {
        RegistroInvalidoException desdePlantilla = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdePlantilla(desdePlantillaPorSemanas(null), HOY));
        RegistroInvalidoException desdeCero = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCeroPorSemanas(null), HOY));

        assertEquals("La cantidad de semanas es obligatoria", desdePlantilla.getMessage());
        assertEquals("La cantidad de semanas es obligatoria", desdeCero.getMessage());
        verify(planRepository, never()).save(any());
        verify(plantillaRepository, never()).sumarAsignacion(anyLong());
    }

    @Test
    void asignar_un_plan_de_cero_semanas_falla() {
        RegistroInvalidoException desdePlantilla = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdePlantilla(desdePlantillaPorSemanas(0), HOY));
        RegistroInvalidoException desdeCero = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCeroPorSemanas(0), HOY));

        assertEquals("El plan tiene que durar al menos una semana", desdePlantilla.getMessage());
        assertEquals(desdePlantilla.getMessage(), desdeCero.getMessage());
        verify(planRepository, never()).save(any());
    }

    @Test
    void asignar_un_plan_de_mas_de_un_anio_falla() {
        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> planService.asignarDesdeCero(desdeCeroPorSemanas(53), HOY));

        assertEquals("El plan no puede durar más de 52 semanas", error.getMessage());
        verify(planRepository, never()).save(any());
    }

    @Test
    void el_dia_de_la_fecha_fin_el_plan_sigue_activo() {
        tieneEnLaBase(planActivoConFin(HOY));

        PlanAsignadoResponse activo = planService.obtenerPlanActivo(7L, HOY).orElseThrow();

        assertEquals(EstadoPlanAsignado.ACTIVO, activo.getEstado());
        assertNull(activo.getFechaFinalizacion());
        // Todavía es su plan activo: no se le puede asignar otro
        assertThrows(ConflictoException.class, () -> planService.asignarDesdeCero(desdeCeroPorSemanas(4), HOY));
    }

    @Test
    void el_dia_siguiente_a_la_fecha_fin_ya_no_es_el_plan_activo_y_figura_finalizado_ese_dia() {
        PlanAsignado plan = planActivoConFin(HOY.minusDays(1));
        tieneEnLaBase(plan);

        assertTrue(planService.obtenerPlanActivo(7L, HOY).isEmpty());
        assertEquals(EstadoPlanAsignado.FINALIZADO, plan.estadoAl(HOY));
        assertEquals(HOY.minusDays(1), plan.fechaFinalizacionAl(HOY));
        // Nadie lo finalizó a mano: en la base sigue ACTIVO hasta la puesta al día
        assertEquals(EstadoPlanAsignado.ACTIVO, plan.getEstado());
    }

    @Test
    void asignar_a_un_alumno_con_un_plan_activo_y_vigente_falla_y_pide_finalizar_el_actual() {
        tieneEnLaBase(planActivoConFin(HOY.plusDays(10)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> planService.asignarDesdeCero(desdeCeroPorSemanas(4), HOY));

        assertTrue(error.getMessage().contains("primero finalizá su plan actual"));
        verify(planRepository, never()).save(any());
    }

    @Test
    void asignar_a_un_alumno_con_el_plan_vencido_pasa_sin_finalizarlo_a_mano() {
        PlanAsignado vencido = planActivoConFin(HOY.minusDays(3));
        vencido.setPlantillaOrigen(plantilla(5L));
        tieneEnLaBase(vencido);

        PlanAsignadoResponse nuevo = planService.asignarDesdeCero(desdeCeroPorSemanas(4), HOY);

        assertEquals(EstadoPlanAsignado.ACTIVO, nuevo.getEstado());
        // El vencido queda cerrado en la base con su fecha fin y libera la plantilla: nunca hay dos ACTIVO
        assertEquals(EstadoPlanAsignado.FINALIZADO, vencido.getEstado());
        assertEquals(HOY.minusDays(3), vencido.getFechaFinalizacion());
        verify(plantillaRepository).restarAsignacion(5L);
    }

    @Test
    void un_plan_viejo_sin_fecha_fin_sigue_vigente_hasta_que_se_finalice_a_mano() {
        PlanAsignado viejo = planActivoConFin(null);
        tieneEnLaBase(viejo);
        LocalDate dentroDeUnAnio = HOY.plusYears(1);

        assertTrue(planService.obtenerPlanActivo(7L, dentroDeUnAnio).isPresent());
        assertThrows(ConflictoException.class,
                () -> planService.asignarDesdeCero(desdeCeroPorSemanas(4), dentroDeUnAnio));
        assertEquals(0, planService.cerrarVencidos(dentroDeUnAnio));

        planService.finalizarPlan(30L, HOY);

        assertEquals(EstadoPlanAsignado.FINALIZADO, viejo.getEstado());
        assertEquals(HOY, viejo.getFechaFinalizacion());
    }

    // --- HU-267: modificar la fecha fin y planes vencidos ---

    private PlanRequest cambiosPorSemanas(Integer semanas) {
        PlanRequest cambios = desdeCero(7L, sesion("Solo remo", ejercicio(2L, 5, 5)));
        cambios.setCantidadSemanas(semanas);
        return cambios;
    }

    // planActivoConFin se asignó el 03/10: el 03/11 va por la semana 5 (del 31/10 al 06/11)

    @Test
    void modificar_las_semanas_de_un_plan_activo_pasa_y_recalcula_la_fecha_fin() {
        PlanAsignado plan = planActivoConFin(HOY.plusDays(5));
        tieneEnLaBase(plan);

        PlanAsignadoResponse modificado = planService.actualizarPlan(30L, cambiosPorSemanas(10), HOY);

        assertEquals(LocalDate.of(2026, 12, 11), modificado.getFechaFin());
        assertEquals(10, modificado.getCantidadSemanas());
        assertEquals(LocalDate.of(2026, 12, 11), plan.getFechaFin());
    }

    @Test
    void modificar_las_semanas_hasta_la_semana_en_curso_pasa() {
        tieneEnLaBase(planActivoConFin(HOY.plusDays(5)));

        assertEquals(LocalDate.of(2026, 11, 6), planService.actualizarPlan(30L, cambiosPorSemanas(5), HOY).getFechaFin());
    }

    @Test
    void modificar_a_menos_semanas_que_la_semana_en_curso_falla() {
        PlanAsignado plan = planActivoConFin(HOY.plusDays(5));
        tieneEnLaBase(plan);

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> planService.actualizarPlan(30L, cambiosPorSemanas(4), HOY));

        assertEquals("El plan ya va por la semana 5: no puede durar menos de 5 semanas", error.getMessage());
        assertEquals(HOY.plusDays(5), plan.getFechaFin());
        verify(planRepository, never()).save(any());
    }

    @Test
    void modificar_sin_semanas_falla() {
        tieneEnLaBase(planActivoConFin(HOY.plusDays(5)));

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> planService.actualizarPlan(30L, cambiosPorSemanas(null), HOY));

        assertEquals("La cantidad de semanas es obligatoria", error.getMessage());
    }

    @Test
    void un_plan_vencido_no_se_puede_modificar() {
        PlanAsignado vencido = planActivoConFin(HOY.minusDays(1));
        tieneEnLaBase(vencido);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> planService.actualizarPlan(30L, cambiosPorSemanas(10), HOY));

        assertEquals("El plan está finalizado y ya no se puede modificar", error.getMessage());
        assertEquals(HOY.minusDays(1), vencido.getFechaFin());
        verify(planRepository, never()).save(any());
    }

    @Test
    void un_plan_vencido_no_se_puede_finalizar_a_mano() {
        tieneEnLaBase(planActivoConFin(HOY.minusDays(1)));

        ConflictoException error = assertThrows(ConflictoException.class, () -> planService.finalizarPlan(30L, HOY));

        assertEquals("El plan ya está finalizado", error.getMessage());
        verify(plantillaRepository, never()).restarAsignacion(anyLong());
    }

    // --- Puesta al día: cerrar los planes vencidos ---

    @Test
    void la_puesta_al_dia_cierra_solo_los_vencidos_con_su_fecha_fin_y_libera_la_plantilla() {
        PlanAsignado vencido = planActivoConFin(HOY.minusDays(2));
        vencido.setPlantillaOrigen(plantilla(5L));
        PlanAsignado venceHoy = planActivoConFin(HOY);
        PlanAsignado viejo = planActivoConFin(null);
        when(planRepository.findByEstado(EstadoPlanAsignado.ACTIVO)).thenReturn(List.of(vencido, venceHoy, viejo));

        assertEquals(1, planService.cerrarVencidos(HOY));

        assertEquals(EstadoPlanAsignado.FINALIZADO, vencido.getEstado());
        assertEquals(HOY.minusDays(2), vencido.getFechaFinalizacion());
        assertEquals(EstadoPlanAsignado.ACTIVO, venceHoy.getEstado());
        assertEquals(EstadoPlanAsignado.ACTIVO, viejo.getEstado());
        verify(plantillaRepository).restarAsignacion(5L);
    }

    @Test
    void la_puesta_al_dia_de_planes_es_idempotente() {
        PlanAsignado vencido = planActivoConFin(HOY.minusDays(2));
        vencido.setPlantillaOrigen(plantilla(5L));
        // La segunda vez el vencido ya está FINALIZADO y la consulta de ACTIVO no lo trae
        when(planRepository.findByEstado(EstadoPlanAsignado.ACTIVO)).thenReturn(List.of(vencido), List.of());

        assertEquals(1, planService.cerrarVencidos(HOY));
        assertEquals(0, planService.cerrarVencidos(HOY));

        verify(plantillaRepository, org.mockito.Mockito.times(1)).restarAsignacion(5L);
    }

    @Test
    void dar_de_baja_a_un_alumno_con_el_plan_vencido_lo_cierra_con_su_fecha_fin() {
        PlanAsignado vencido = planActivoConFin(ZonaHoraria.hoy().minusDays(4));
        tieneEnLaBase(vencido);

        planService.finalizarPlanActivoDe(7L);

        assertEquals(EstadoPlanAsignado.FINALIZADO, vencido.getEstado());
        assertEquals(ZonaHoraria.hoy().minusDays(4), vencido.getFechaFinalizacion());
    }
}
