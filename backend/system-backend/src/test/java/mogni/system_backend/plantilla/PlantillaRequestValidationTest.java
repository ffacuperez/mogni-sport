package mogni.system_backend.plantilla;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.PlantillaRequest;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/** HU-265: validaciones del cuerpo de alta/modificación de plantillas (las que devuelven 400 en el controller). */
class PlantillaRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private EjercicioSesionRequest ejercicio;
    private SesionRequest sesion;

    private PlantillaRequest requestValido() {
        ejercicio = new EjercicioSesionRequest();
        ejercicio.setEjercicioId(1L);
        ejercicio.setSeries(4);
        ejercicio.setRepeticiones(10);
        ejercicio.setPesoSugerido(new BigDecimal("40"));
        ejercicio.setRirObjetivo(2);
        sesion = new SesionRequest();
        sesion.setNombre("Full body A");
        sesion.setEjercicios(new ArrayList<>(List.of(ejercicio)));
        PlantillaRequest request = new PlantillaRequest();
        request.setNombre("Fuerza base");
        request.setNivel(NivelPlantilla.PRINCIPIANTE);
        request.setObjetivo(ObjetivoPlantilla.FUERZA);
        request.setSesiones(new ArrayList<>(List.of(sesion)));
        return request;
    }

    private boolean tieneError(PlantillaRequest request, String mensaje) {
        Set<ConstraintViolation<PlantillaRequest>> errores = validator.validate(request);
        return errores.stream().anyMatch(v -> v.getMessage().equals(mensaje));
    }

    @Test
    void requestValidoNoTieneErrores() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void sesionNulaEnLaListaFalla() {
        PlantillaRequest request = requestValido();
        request.getSesiones().add(null);
        assertTrue(tieneError(request, "Cada sesión tiene que tener al menos un ejercicio"));
    }

    @Test
    void ejercicioNuloEnLaListaFalla() {
        PlantillaRequest request = requestValido();
        sesion.getEjercicios().add(null);
        assertTrue(tieneError(request, "Cada ejercicio de la sesión tiene que ser uno del catálogo"));
    }

    @Test
    void nombreSoloConEspaciosDurosFalla() {
        PlantillaRequest request = requestValido();
        request.setNombre("  ");
        assertTrue(tieneError(request, "El nombre de la plantilla es obligatorio"));
    }

    @Test
    void sinNombreDescriptivoDeSesionNiPesoNiRirSigueSiendoValido() {
        PlantillaRequest request = requestValido();
        sesion.setNombre(null);
        ejercicio.setPesoSugerido(null);
        ejercicio.setRirObjetivo(null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void sinSesionesFalla() {
        PlantillaRequest request = requestValido();
        request.setSesiones(List.of());
        assertTrue(tieneError(request, "La plantilla tiene que tener al menos una sesión"));
    }

    @Test
    void sesionSinEjerciciosFalla() {
        PlantillaRequest request = requestValido();
        sesion.setEjercicios(List.of());
        assertTrue(tieneError(request, "Cada sesión tiene que tener al menos un ejercicio"));
    }

    @Test
    void nombreNivelYObjetivoSonObligatorios() {
        PlantillaRequest request = requestValido();
        request.setNombre("  ");
        request.setNivel(null);
        request.setObjetivo(null);
        assertTrue(tieneError(request, "El nombre de la plantilla es obligatorio"));
        assertTrue(tieneError(request, "El nivel es obligatorio"));
        assertTrue(tieneError(request, "El objetivo es obligatorio"));
    }

    @Test
    void seriesYRepeticionesTienenQueSerAlMenosUno() {
        PlantillaRequest request = requestValido();
        ejercicio.setSeries(0);
        ejercicio.setRepeticiones(0);
        assertTrue(tieneError(request, "Las series tienen que ser al menos 1"));
        assertTrue(tieneError(request, "Las repeticiones objetivo tienen que ser al menos 1"));
    }

    @Test
    void repeticionesYSeriesSonObligatorias() {
        PlantillaRequest request = requestValido();
        ejercicio.setSeries(null);
        ejercicio.setRepeticiones(null);
        assertTrue(tieneError(request, "Las series son obligatorias"));
        assertTrue(tieneError(request, "Las repeticiones objetivo son obligatorias"));
    }

    @Test
    void pesoSugeridoTieneQueSerPositivoYConHastaDosDecimales() {
        PlantillaRequest request = requestValido();
        ejercicio.setPesoSugerido(BigDecimal.ZERO);
        assertTrue(tieneError(request, "El peso sugerido tiene que ser mayor a 0"));
        ejercicio.setPesoSugerido(new BigDecimal("12.345"));
        assertTrue(tieneError(request, "El peso sugerido admite hasta 2 decimales"));
    }

    @Test
    void rirObjetivoVaDe0A10() {
        PlantillaRequest request = requestValido();
        ejercicio.setRirObjetivo(0);
        assertTrue(validator.validate(request).isEmpty());
        ejercicio.setRirObjetivo(-1);
        assertTrue(tieneError(request, "El RIR objetivo no puede ser negativo"));
        ejercicio.setRirObjetivo(11);
        assertTrue(tieneError(request, "El RIR objetivo no puede ser más de 10"));
    }

    @Test
    void rangoDeRepeticionesConMinimoMayorAlMaximoFalla() {
        PlantillaRequest request = requestValido();
        ejercicio.setRepeticiones(10);
        ejercicio.setRepeticionesMax(8);
        assertTrue(tieneError(request, "En el rango de repeticiones, el mínimo no puede ser mayor al máximo"));
    }

    @Test
    void rangoDeRepeticionesValidoOSinMaximoPasa() {
        PlantillaRequest request = requestValido();
        ejercicio.setRepeticiones(8);
        ejercicio.setRepeticionesMax(10);
        assertTrue(validator.validate(request).isEmpty());
        ejercicio.setRepeticionesMax(null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void descansoSugeridoEnMinutosMayorA0YHasta10() {
        PlantillaRequest request = requestValido();
        ejercicio.setDescansoMinutos(new BigDecimal("1.5"));
        assertTrue(validator.validate(request).isEmpty());
        ejercicio.setDescansoMinutos(BigDecimal.ZERO);
        assertTrue(tieneError(request, "El descanso sugerido tiene que ser mayor a 0"));
        ejercicio.setDescansoMinutos(new BigDecimal("10.5"));
        assertTrue(tieneError(request, "El descanso sugerido no puede superar los 10 minutos"));
        ejercicio.setDescansoMinutos(new BigDecimal("1.255"));
        assertTrue(tieneError(request, "El descanso sugerido admite hasta 2 decimales"));
    }

    @Test
    void ejercicioSinIdDelCatalogoFalla() {
        PlantillaRequest request = requestValido();
        ejercicio.setEjercicioId(null);
        assertTrue(tieneError(request, "Cada ejercicio de la sesión tiene que ser uno del catálogo"));
    }
}
