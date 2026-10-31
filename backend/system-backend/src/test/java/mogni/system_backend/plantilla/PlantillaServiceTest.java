package mogni.system_backend.plantilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.EjercicioSesion;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import mogni.system_backend.plantilla.entity.PlantillaPlan;
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
            1L, new Ejercicio(1L, "Press de banca con barra", "Pecho", null, true),
            2L, new Ejercicio(2L, "Remo con mancuerna", "Espalda", null, true),
            3L, new Ejercicio(3L, "Sentadilla", "Piernas", null, true),
            9L, new Ejercicio(9L, "Peso muerto rumano", "Piernas", null, false));

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

    private static PlantillaRequest request(String nombre, SesionRequest... sesiones) {
        PlantillaRequest r = new PlantillaRequest();
        r.setNombre(nombre);
        r.setNivel(NivelPlantilla.INTERMEDIO);
        r.setObjetivo(ObjetivoPlantilla.HIPERTROFIA);
        r.setSesiones(new ArrayList<>(List.of(sesiones)));
        return r;
    }

    /** Plantilla guardada con una sesión de press de banca 4x10 40 kg. */
    private PlantillaPlan plantillaExistente(Long id, String nombre, int asignaciones) {
        PlantillaPlan p = new PlantillaPlan();
        p.setId(id);
        p.setNombre(nombre);
        p.setNivel(NivelPlantilla.PRINCIPIANTE);
        p.setObjetivo(ObjetivoPlantilla.FUERZA);
        p.setAsignacionesActivas(asignaciones);
        p.setFechaActualizacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        SesionPlantilla s = new SesionPlantilla("Full body A");
        s.agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(1L), 4, 10, new BigDecimal("40"), 2)));
        p.reemplazarSesiones(new ArrayList<>(List.of(s)));
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
        assertEquals(2, creada.getSesiones().size());
        // Las sesiones quedan numeradas en orden y el nombre es opcional
        assertEquals(1, creada.getSesiones().get(0).getNumero());
        assertEquals("Empuje", creada.getSesiones().get(0).getNombre());
        assertEquals(2, creada.getSesiones().get(1).getNumero());
        assertNull(creada.getSesiones().get(1).getNombre());
        var primerEjercicio = creada.getSesiones().get(0).getEjercicios().get(0);
        assertEquals("Press de banca con barra", primerEjercicio.getNombre());
        assertEquals(4, primerEjercicio.getSeries());
        assertEquals(10, primerEjercicio.getRepeticiones());
        assertEquals(new BigDecimal("40"), primerEjercicio.getPesoSugerido());
        assertEquals(2, primerEjercicio.getRirObjetivo());
        assertNull(creada.getSesiones().get(0).getEjercicios().get(1).getPesoSugerido());
        assertNull(creada.getSesiones().get(0).getEjercicios().get(1).getRirObjetivo());
    }

    @Test
    void alta_sin_ninguna_sesion_falla() {
        assertThrows(RegistroInvalidoException.class, () -> plantillaService.crearPlantilla(request("Vacía")));
        verify(plantillaRepository, never()).save(any());
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
        assertEquals(2, modificada.getSesiones().size());
        assertEquals(5, modificada.getSesiones().get(0).getEjercicios().get(0).getSeries());
        assertTrue(existente.getFechaActualizacion().isAfter(LocalDateTime.of(2026, 10, 1, 10, 0)));
    }

    @Test
    void modificacion_puede_conservar_un_ejercicio_que_se_dio_de_baja_despues_de_agregarlo() {
        PlantillaPlan existente = plantillaExistente(5L, "Fuerza base", 0);
        existente.getSesiones().get(0).agregarEjercicios(List.of(new EjercicioSesion(catalogo.get(9L), 3, 8, null, null)));
        when(plantillaRepository.findByIdAndActivaTrue(5L)).thenReturn(Optional.of(existente));

        PlantillaResponse modificada = plantillaService.actualizarPlantilla(5L, request("Fuerza base",
                sesion("A", ejercicio(1L, 4, 10, "40"), ejercicio(9L, 4, 8, null))));

        assertFalse(modificada.getSesiones().get(0).getEjercicios().get(1).isEjercicioActivo());
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
        assertEquals(2, nueva.getSesiones().get(0).getEjercicios().get(0).getRirObjetivo());
        // Sesiones y ejercicios son objetos nuevos: cambiar la copia no toca la original
        assertNotSame(original.getSesiones().get(0), nueva.getSesiones().get(0));
        nueva.getSesiones().get(0).getEjercicios().get(0).setSeries(8);
        nueva.getSesiones().get(0).setNombre("Cambiada");
        assertEquals(4, original.getSesiones().get(0).getEjercicios().get(0).getSeries());
        assertEquals("Full body A", original.getSesiones().get(0).getNombre());
        assertEquals(2, original.getAsignacionesActivas());
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
        assertEquals(1, lista.get(0).getSesiones().get(0).getEjercicios().size());
    }
}
