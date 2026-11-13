package mogni.system_backend.plantilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.dao.DataIntegrityViolationException;

import mogni.system_backend.ejercicio.entity.Ejercicio;
import mogni.system_backend.ejercicio.repository.EjercicioRepository;
import mogni.system_backend.exception.ConflictoException;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.PlantillaRequest;
import mogni.system_backend.plantilla.dto.PlantillaResponse;
import mogni.system_backend.plantilla.dto.SemanaRequest;
import mogni.system_backend.plantilla.dto.SesionPlantillaResponse;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.EjercicioSesion;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
import mogni.system_backend.plantilla.entity.SemanaPlantilla;
import mogni.system_backend.plantilla.entity.SesionPlantilla;
import mogni.system_backend.plantilla.repository.PlantillaPlanRepository;
import mogni.system_backend.plantilla.service.PlantillaService;

/** HU-265: pruebas de aceptación de "Gestionar plantillas de plan de entrenamiento" a nivel servicio. */
@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    @Mock
    private PlantillaPlanRepository plantillaRepository;

    @Mock
    private EjercicioRepository ejercicioRepository;

    @InjectMocks
    private PlantillaService plantillaService;

    private final Map<Long, Ejercicio> catalogo = Map.of(
            1L, new Ejercicio(1L, "Press de banca con barra", "Pecho", true),
            2L, new Ejercicio(2L, "Remo con mancuerna", "Espalda", true),
            3L, new Ejercicio(3L, "Sentadilla", "Piernas", true),
            9L, new Ejercicio(9L, "Peso muerto rumano", "Piernas", false));

    @BeforeEach
    void setUp() {
        lenient().when(plantillaRepository.save(any(PlantillaPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ejercicioRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return ids.stream().filter(catalogo::containsKey).map(catalogo::get).toList();
        });
    }

    // --- Datos de prueba ---

    private static EjercicioSesionRequest ejercicio(Long id, int series, int repeticiones, String peso) {
        EjercicioSesionRequest e = new EjercicioSesionRequest();
        e.setEjercicioId(id);
        e.setSeries(series);
        e.setRepeticiones(repeticiones);
        e.setPesoSugerido(peso == null ? null : new BigDecimal(peso));
        return e;
    }

    private static SesionRequest sesion(String nombre, EjercicioSesionRequest... ejercicios) {
        SesionRequest s = new SesionRequest();
        s.setNombre(nombre);
        s.setEjercicios(new ArrayList<>(List.of(ejercicios)));
        return s;
    }

    private static SemanaRequest semana(String nota, SesionRequest... sesiones) {
        SemanaRequest s = new SemanaRequest();
        s.setNota(nota);
        s.setSesiones(new ArrayList<>(List.of(sesiones)));
        return s;
    }

    /** Plantilla de una sola semana con esas sesiones. */
    private static PlantillaRequest request(String nombre, SesionRequest... sesiones) {
        return requestConSemanas(nombre, semana(null, sesiones));
    }

    private static PlantillaRequest requestConSemanas(String nombre, SemanaRequest... semanas) {
        PlantillaRequest r = new PlantillaRequest();
        r.setNombre(nombre);
        r.setNivel(NivelPlantilla.INTERMEDIO);
        r.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        r.setSemanas(new ArrayList<>(List.of(semanas)));
        return r;
    }

    /** Las sesiones de la semana 1, de una respuesta o de una plantilla. */
    private static List<SesionPlantillaResponse> semana1(PlantillaResponse plantilla) {
        return plantilla.getSemanas().get(0).getSesiones();
    }

    private static List<SesionPlantilla> semana1(PlantillaPlan plantilla) {
        return plantilla.getSemanas().get(0).getSesiones();
    }

    /** Plantilla guardada con una semana y una sesión de press de banca 4x10 40 kg. */
    private PlantillaPlan plantillaExistente(Long id, String nombre, int asignaciones) {
        PlantillaPlan p = new PlantillaPlan();
        p.setId(id);
        p.setNombre(nombre);
        p.setNivel(NivelPlantilla.PRINCIPIANTE);
        p.setObjetivo(ObjetivoPlantilla.FUERZA);
        p.setAsignacionesActivas(asignaciones);
        p.setFechaActualizacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        SesionPlantilla s = new SesionPlantilla("Full body A");
        s.agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(1L), 4, 8, 10, new BigDecimal("1.5"), new BigDecimal("40"), 2)));
        SemanaPlantilla semana = new SemanaPlantilla("Semana base");
        semana.agregarSesiones(List.of(s));
        p.agregarSemanas(List.of(semana));
        return p;
    }

    // --- Alta ---

    @Test
    void alta_con_nivel_objetivo_y_una_sesion_con_ejercicios_pasa() {
        EjercicioSesionRequest conRir = ejercicio(1L, 4, 10, "40");
        conRir.setRirObjetivo(2);
        PlantillaResponse creada = plantillaService.crearPlantilla(request("  Hipertrofia · tren superior  ",
                sesion("Empuje", conRir, ejercicio(2L, 3, 12, null)),
                sesion(null, ejercicio(3L, 5, 5, "80.5"))));

        assertEquals("Hipertrofia · tren superior", creada.getNombre());
        assertEquals(NivelPlantilla.INTERMEDIO, creada.getNivel());
        assertEquals(ObjetivoPlantilla.HIPERTROFIA, creada.getObjetivo());
        assertEquals(0, creada.getAsignacionesActivas());
        assertEquals(2, semana1(creada).size());
        // Las sesiones quedan numeradas en orden y el nombre es opcional
        assertEquals(1, semana1(creada).get(0).getNumero());
        assertEquals("Empuje", semana1(creada).get(0).getNombre());
        assertEquals(2, semana1(creada).get(1).getNumero());
        assertNull(semana1(creada).get(1).getNombre());
        var primerEjercicio = semana1(creada).get(0).getEjercicios().get(0);
        assertEquals("Press de banca con barra", primerEjercicio.getNombre());
        assertEquals(4, primerEjercicio.getSeries());
        assertEquals(10, primerEjercicio.getRepeticiones());
        assertEquals(new BigDecimal("40"), primerEjercicio.getPesoSugerido());
        assertEquals(2, primerEjercicio.getRirObjetivo());
        assertNull(semana1(creada).get(0).getEjercicios().get(1).getPesoSugerido());
        assertNull(semana1(creada).get(0).getEjercicios().get(1).getRirObjetivo());
    }

    @Test
    void alta_con_rango_de_repeticiones_y_descanso_pasa() {
        EjercicioSesionRequest conRango = ejercicio(1L, 4, 8, null);
        conRango.setRepeticionesMax(10);
        conRango.setDescansoMinutos(new BigDecimal("1.5"));

        var creado = semana1(plantillaService.crearPlantilla(request("Rango", sesion(null, conRango))))
                .get(0).getEjercicios().get(0);

        assertEquals(8, creado.getRepeticiones());
        assertEquals(10, creado.getRepeticionesMax());
        assertEquals(new BigDecimal("1.5"), creado.getDescansoMinutos());
    }

    @Test
    void alta_con_un_solo_valor_de_repeticiones_pasa_como_objetivo_fijo() {
        var creado = semana1(plantillaService.crearPlantilla(request("Fijo", sesion(null, ejercicio(1L, 4, 8, null)))))
                .get(0).getEjercicios().get(0);

        assertEquals(8, creado.getRepeticiones());
        assertNull(creado.getRepeticionesMax());
        assertNull(creado.getDescansoMinutos());
    }

    @Test
    void un_rango_con_minimo_igual_al_maximo_se_guarda_como_objetivo_fijo() {
        EjercicioSesionRequest mismoValor = ejercicio(1L, 4, 8, null);
        mismoValor.setRepeticionesMax(8);

        var creado = semana1(plantillaService.crearPlantilla(request("Igual", sesion(null, mismoValor))))
                .get(0).getEjercicios().get(0);

        assertEquals(8, creado.getRepeticiones());
        assertNull(creado.getRepeticionesMax());
    }

    @Test
    void alta_sin_ninguna_semana_falla() {
        assertThrows(RegistroInvalidoException.class, () -> plantillaService.crearPlantilla(requestConSemanas("Vacía")));
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void alta_con_una_semana_sin_sesiones_falla() {
        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Semana vacía")));
        assertEquals("Cada semana tiene que tener al menos una sesión", error.getMessage());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void alta_con_semanas_distintas_guarda_cada_una_con_sus_sesiones_y_su_nota() {
        EjercicioSesionRequest pesado = ejercicio(3L, 5, 5, "80");
        pesado.setIndicaciones("  Tempo 2-1-3  ");
        EjercicioSesionRequest descarga = ejercicio(3L, 3, 5, "60");
        descarga.setRirObjetivo(4);

        PlantillaResponse creada = plantillaService.crearPlantilla(requestConSemanas("Fuerza por semanas",
                semana("  ", sesion("Pierna", pesado), sesion("Torso", ejercicio(1L, 4, 8, null))),
                semana("  Semana de descarga ", sesion("Pierna liviana", descarga))));

        assertEquals(2, creada.getSemanas().size());
        var primera = creada.getSemanas().get(0);
        var segunda = creada.getSemanas().get(1);
        assertEquals(1, primera.getNumero());
        assertNull(primera.getNota());
        assertEquals(2, primera.getSesiones().size());
        assertEquals("Tempo 2-1-3", primera.getSesiones().get(0).getEjercicios().get(0).getIndicaciones());
        assertNull(primera.getSesiones().get(1).getEjercicios().get(0).getIndicaciones());
        // La semana 2 tiene su propio contenido y numera sus sesiones desde 1
        assertEquals(2, segunda.getNumero());
        assertEquals("Semana de descarga", segunda.getNota());
        assertEquals(1, segunda.getSesiones().size());
        assertEquals(1, segunda.getSesiones().get(0).getNumero());
        assertEquals(3, segunda.getSesiones().get(0).getEjercicios().get(0).getSeries());
        assertEquals(4, segunda.getSesiones().get(0).getEjercicios().get(0).getRirObjetivo());
    }

    @Test
    void alta_con_mas_de_24_semanas_falla() {
        SemanaRequest[] semanas = new SemanaRequest[PlantillaPlan.MAXIMO_SEMANAS + 1];
        for (int i = 0; i < semanas.length; i++) {
            semanas[i] = semana(null, sesion(null, ejercicio(1L, 3, 10, null)));
        }

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(requestConSemanas("Muy larga", semanas)));
        assertEquals("La plantilla puede tener como máximo 24 semanas", error.getMessage());
    }

    @Test
    void alta_con_mas_de_7_sesiones_en_una_semana_falla() {
        SesionRequest[] sesiones = new SesionRequest[PlantillaPlan.MAXIMO_SESIONES_POR_SEMANA + 1];
        for (int i = 0; i < sesiones.length; i++) {
            sesiones[i] = sesion(null, ejercicio(1L, 3, 10, null));
        }

        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Muchas sesiones", sesiones)));
        assertEquals("Cada semana puede tener como máximo 7 sesiones", error.getMessage());
    }

    @Test
    void alta_con_una_sesion_sin_ejercicios_falla() {
        assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Sin ejercicios", sesion("Sesión vacía"))));
    }

    @Test
    void alta_con_nombre_repetido_falla() {
        when(plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue("Fuerza base")).thenReturn(true);

        assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Fuerza base", sesion(null, ejercicio(1L, 3, 10, null)))));
    }

    @Test
    void alta_con_un_ejercicio_que_no_esta_en_el_catalogo_falla() {
        assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Nueva", sesion(null, ejercicio(77L, 3, 10, null)))));
    }

    @Test
    void alta_con_un_ejercicio_dado_de_baja_falla() {
        RegistroInvalidoException error = assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.crearPlantilla(request("Nueva", sesion(null, ejercicio(9L, 3, 10, null)))));
        assertTrue(error.getMessage().contains("Peso muerto rumano"));
    }

    // --- Modificación ---

    @Test
    void modificacion_de_una_plantilla_sin_asignaciones_activas_pasa() {
        PlantillaPlan existente = plantillaExistente(5L, "Fuerza base", 0);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(existente));

        PlantillaResponse modificada = plantillaService.actualizarPlantilla(5L, request("Fuerza base · v2",
                sesion("A", ejercicio(1L, 5, 5, "60")),
                sesion("B", ejercicio(3L, 5, 5, "90"))));

        assertEquals("Fuerza base · v2", modificada.getNombre());
        assertEquals(NivelPlantilla.INTERMEDIO, modificada.getNivel());
        assertEquals(2, semana1(modificada).size());
        assertEquals(5, semana1(modificada).get(0).getEjercicios().get(0).getSeries());
        assertTrue(existente.getFechaActualizacion().isAfter(LocalDateTime.of(2026, 10, 1, 10, 0)));
    }

    @Test
    void modificacion_borra_las_semanas_viejas_con_un_flush_antes_de_agregar_las_nuevas() {
        PlantillaPlan existente = plantillaExistente(5L, "Fuerza base", 0);
        SemanaPlantilla vieja = existente.getSemanas().get(0);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(existente));
        // Al momento del primer flush la plantilla ya no tiene semanas: así los DELETE salen antes que los INSERT
        List<Integer> semanasAlHacerFlush = new ArrayList<>();
        doAnswer(inv -> {
            semanasAlHacerFlush.add(existente.getSemanas().size());
            return null;
        }).when(plantillaRepository).flush();

        plantillaService.actualizarPlantilla(5L, requestConSemanas("Fuerza base",
                semana(null, sesion("A", ejercicio(1L, 5, 5, "60"))),
                semana("Descarga", sesion("A", ejercicio(1L, 3, 5, "50")))));

        assertEquals(List.of(0, 2), semanasAlHacerFlush);
        assertEquals(2, existente.getSemanas().size());
        assertFalse(existente.getSemanas().contains(vieja));
        assertEquals("Descarga", existente.getSemanas().get(1).getNota());
    }

    @Test
    void modificacion_puede_conservar_un_ejercicio_que_se_dio_de_baja_despues_de_agregarlo() {
        PlantillaPlan existente = plantillaExistente(5L, "Fuerza base", 0);
        semana1(existente).get(0).agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(9L), 3, 8, null, null, null, null)));
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(existente));

        PlantillaResponse modificada = plantillaService.actualizarPlantilla(5L, request("Fuerza base",
                sesion("A", ejercicio(1L, 4, 10, "40"), ejercicio(9L, 4, 8, null))));

        assertFalse(semana1(modificada).get(0).getEjercicios().get(1).isEjercicioActivo());
    }

    @Test
    void modificacion_con_el_nombre_de_otra_plantilla_falla() {
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantillaExistente(5L, "Fuerza base", 0)));
        when(plantillaRepository.existsByNombreIgnoreCaseAndActivaTrueAndIdNot("Definición", 5L)).thenReturn(true);

        assertThrows(RegistroInvalidoException.class,
                () -> plantillaService.actualizarPlantilla(5L, request("Definición", sesion(null, ejercicio(1L, 3, 10, null)))));
    }

    @Test
    void modificacion_de_una_plantilla_inexistente_o_dada_de_baja_da_404() {
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> plantillaService.actualizarPlantilla(5L, request("X", sesion(null, ejercicio(1L, 3, 10, null)))));
    }

    // --- Eliminación (baja lógica) ---

    @Test
    void eliminacion_de_una_plantilla_con_alumnos_asignados_actualmente_falla() {
        PlantillaPlan asignada = plantillaExistente(5L, "Fuerza base", 3);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(asignada));

        ConflictoException error = assertThrows(ConflictoException.class, () -> plantillaService.eliminarPlantilla(5L));
        assertTrue(error.getMessage().contains("3 alumnos"));
        assertTrue(asignada.isActiva());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void eliminacion_sin_asignaciones_es_una_baja_logica() {
        PlantillaPlan plantilla = plantillaExistente(5L, "Fuerza base", 0);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantilla));

        plantillaService.eliminarPlantilla(5L);

        assertFalse(plantilla.isActiva());
        verify(plantillaRepository).save(plantilla);
        verify(plantillaRepository, never()).delete(any());
    }

    // --- Duplicado ---

    @Test
    void duplicar_una_plantilla_crea_una_copia_independiente() {
        PlantillaPlan original = plantillaExistente(5L, "Fuerza base", 2);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(original));

        PlantillaResponse copia = plantillaService.duplicarPlantilla(5L);

        ArgumentCaptor<PlantillaPlan> guardada = ArgumentCaptor.forClass(PlantillaPlan.class);
        verify(plantillaRepository).save(guardada.capture());
        PlantillaPlan nueva = guardada.getValue();

        assertEquals("Fuerza base (copia)", copia.getNombre());
        assertEquals(0, copia.getAsignacionesActivas());
        assertEquals(original.getNivel(), nueva.getNivel());
        assertEquals(original.getObjetivo(), nueva.getObjetivo());
        assertEquals(2, semana1(nueva).get(0).getEjercicios().get(0).getRirObjetivo());
        // Sesiones y ejercicios son objetos nuevos: cambiar la copia no toca la original
        assertNotSame(semana1(original).get(0), semana1(nueva).get(0));
        semana1(nueva).get(0).getEjercicios().get(0).setSeries(8);
        semana1(nueva).get(0).setNombre("Cambiada");
        assertEquals(4, semana1(original).get(0).getEjercicios().get(0).getSeries());
        var ejercicioCopiado = semana1(nueva).get(0).getEjercicios().get(0);
        assertEquals(10, ejercicioCopiado.getRepeticionesMax());
        assertEquals(new BigDecimal("1.5"), ejercicioCopiado.getDescansoMinutos());
        assertEquals("Full body A", semana1(original).get(0).getNombre());
        assertEquals(2, original.getAsignacionesActivas());
    }

    @Test
    void duplicar_copia_todas_las_semanas_con_su_nota() {
        PlantillaPlan original = plantillaExistente(5L, "Fuerza base", 0);
        SesionPlantilla descarga = new SesionPlantilla("Descarga");
        descarga.agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(3L), 2, 5, null, null, null, 4)));
        SemanaPlantilla semana2 = new SemanaPlantilla("Semana de descarga");
        semana2.agregarSesiones(List.of(descarga));
        original.agregarSemanas(List.of(semana2));
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(original));

        PlantillaResponse copia = plantillaService.duplicarPlantilla(5L);

        assertEquals(2, copia.getSemanas().size());
        assertEquals("Semana base", copia.getSemanas().get(0).getNota());
        assertEquals(2, copia.getSemanas().get(1).getNumero());
        assertEquals("Semana de descarga", copia.getSemanas().get(1).getNota());
        assertEquals("Descarga", copia.getSemanas().get(1).getSesiones().get(0).getNombre());
        assertEquals(4, copia.getSemanas().get(1).getSesiones().get(0).getEjercicios().get(0).getRirObjetivo());
    }

    @Test
    void duplicar_dos_veces_numera_las_copias() {
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantillaExistente(5L, "Fuerza base (copia)", 0)));
        when(plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue("Fuerza base (copia)")).thenReturn(true);

        assertEquals("Fuerza base (copia 2)", plantillaService.duplicarPlantilla(5L).getNombre());
    }

    @Test
    void la_copia_de_un_nombre_largo_respeta_el_maximo() {
        String largo = "N".repeat(100);
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantillaExistente(5L, largo, 0)));

        String copia = plantillaService.duplicarPlantilla(5L).getNombre();

        assertEquals(100, copia.length());
        assertEquals("N".repeat(92) + " (copia)", copia);
    }

    @Test
    void la_copia_de_un_nombre_que_es_solo_el_sufijo_no_empieza_con_espacio() {
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantillaExistente(5L, "(copia 7)", 0)));

        assertEquals("(copia)", plantillaService.duplicarPlantilla(5L).getNombre());
    }

    @Test
    void si_otra_alta_simultanea_tomo_el_nombre_responde_como_nombre_repetido() {
        doThrow(new DataIntegrityViolationException("nombre_unico")).when(plantillaRepository).flush();
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(plantillaExistente(5L, "Fuerza base", 0)));

        RegistroInvalidoException e = assertThrows(RegistroInvalidoException.class, () -> plantillaService.duplicarPlantilla(5L));

        assertEquals("Ya existe una plantilla con ese nombre", e.getMessage());
    }

    @Test
    void el_nombre_unico_sigue_al_nombre_y_se_libera_con_la_baja() {
        PlantillaPlan p = new PlantillaPlan();

        p.setNombre("Fuerza BASE");
        assertEquals("fuerza base", p.getNombreUnico());

        p.setActiva(false);
        assertNull(p.getNombreUnico());
    }

    // --- Consulta ---

    @Test
    void listar_devuelve_las_plantillas_activas_en_el_orden_del_repositorio() {
        when(plantillaRepository.findByActivaTrueOrderByFechaActualizacionDesc())
                .thenReturn(List.of(plantillaExistente(2L, "B", 0), plantillaExistente(1L, "A", 0)));

        List<PlantillaResponse> lista = plantillaService.listarPlantillas();

        assertEquals(List.of("B", "A"), lista.stream().map(PlantillaResponse::getNombre).toList());
        assertEquals(1, semana1(lista.get(0)).get(0).getEjercicios().size());
    }

    // --- Plantillas inactivas ---

    @Test
    void listar_inactivas_devuelve_las_plantillas_dadas_de_baja() {
        PlantillaPlan baja = plantillaExistente(7L, "Circuito", 0);
        baja.setActiva(false);
        when(plantillaRepository.findByActivaFalseOrderByFechaActualizacionDesc()).thenReturn(List.of(baja));

        List<PlantillaResponse> lista = plantillaService.listarInactivas();

        assertEquals(List.of("Circuito"), lista.stream().map(PlantillaResponse::getNombre).toList());
    }

    @Test
    void reactivar_vuelve_a_activar_la_plantilla_y_recupera_su_nombre_unico() {
        PlantillaPlan baja = plantillaExistente(7L, "Circuito", 0);
        baja.setActiva(false);
        when(plantillaRepository.findByIdAndActivaFalse(7L)).thenReturn(Optional.of(baja));
        when(plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue("Circuito")).thenReturn(false);

        plantillaService.reactivarPlantilla(7L);

        assertTrue(baja.isActiva());
        assertEquals("circuito", baja.getNombreUnico());
        assertTrue(baja.getFechaActualizacion().isAfter(LocalDateTime.of(2026, 10, 1, 10, 0)));
    }

    @Test
    void reactivar_con_el_nombre_ocupado_por_una_activa_da_409_y_no_la_reactiva() {
        PlantillaPlan baja = plantillaExistente(7L, "Circuito", 0);
        baja.setActiva(false);
        when(plantillaRepository.findByIdAndActivaFalse(7L)).thenReturn(Optional.of(baja));
        when(plantillaRepository.existsByNombreIgnoreCaseAndActivaTrue("Circuito")).thenReturn(true);

        ConflictoException e = assertThrows(ConflictoException.class, () -> plantillaService.reactivarPlantilla(7L));

        assertEquals("Ya existe una plantilla activa con ese nombre", e.getMessage());
        assertFalse(baja.isActiva());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void reactivar_una_plantilla_activa_o_inexistente_da_404() {
        when(plantillaRepository.findByIdAndActivaFalse(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> plantillaService.reactivarPlantilla(99L));
    }
}
