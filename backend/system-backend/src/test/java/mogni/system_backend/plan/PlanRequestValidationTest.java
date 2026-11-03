package mogni.system_backend.plan;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mogni.system_backend.plan.dto.AsignarPlanDesdeCeroRequest;
import mogni.system_backend.plan.dto.AsignarPlanDesdePlantillaRequest;
import mogni.system_backend.plan.dto.PlanRequest;
import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.SesionRequest;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;

/** HU-266: el plan desde cero tiene las mismas reglas que una plantilla. */
class PlanRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AsignarPlanDesdeCeroRequest requestValido() {
        EjercicioSesionRequest ejercicio = new EjercicioSesionRequest();
        ejercicio.setEjercicioId(1L);
        ejercicio.setSeries(4);
        ejercicio.setRepeticiones(10);
        SesionRequest sesion = new SesionRequest();
        sesion.setEjercicios(new ArrayList<>(List.of(ejercicio)));
        AsignarPlanDesdeCeroRequest request = new AsignarPlanDesdeCeroRequest();
        request.setAlumnoId(7L);
        request.setNombre("Plan de Juan");
        request.setNivel(NivelPlantilla.PRINCIPIANTE);
        request.setObjetivo(ObjetivoPlantilla.FUERZA);
        request.setSesiones(new ArrayList<>(List.of(sesion)));
        request.setFechaFin(LocalDate.of(2099, 12, 31));
        return request;
    }

    private boolean tieneError(Object request, String mensaje) {
        return validator.validate(request).stream().anyMatch(v -> v.getMessage().equals(mensaje));
    }

    @Test
    void planDesdeCeroValidoNoTieneErrores() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void planDesdeCeroSinSesionesFalla() {
        AsignarPlanDesdeCeroRequest request = requestValido();
        request.setSesiones(new ArrayList<>());
        assertTrue(tieneError(request, "El plan tiene que tener al menos una sesión"));
    }

    @Test
    void planDesdeCeroConUnaSesionSinEjerciciosFalla() {
        AsignarPlanDesdeCeroRequest request = requestValido();
        request.getSesiones().get(0).setEjercicios(new ArrayList<>());
        assertTrue(tieneError(request, "Cada sesión tiene que tener al menos un ejercicio"));
    }

    @Test
    void sinAlumnoNiDatosFalla() {
        AsignarPlanDesdeCeroRequest request = requestValido();
        request.setAlumnoId(null);
        request.setNombre(" ");
        request.setNivel(null);
        assertTrue(tieneError(request, "Elegí el alumno al que le vas a asignar el plan"));
        assertTrue(tieneError(request, "El nombre del plan es obligatorio"));
        assertTrue(tieneError(request, "El nivel es obligatorio"));
    }

    @Test
    void desdePlantillaSinAlumnoNiPlantillaFalla() {
        AsignarPlanDesdePlantillaRequest request = new AsignarPlanDesdePlantillaRequest();
        assertTrue(tieneError(request, "Elegí el alumno al que le vas a asignar el plan"));
        assertTrue(tieneError(request, "Elegí la plantilla que le vas a asignar"));
    }

    // --- HU-266: fecha fin obligatoria al asignar (desde cero, desde plantilla) y al modificar ---

    @Test
    void planDesdeCeroSinFechaFinFalla() {
        AsignarPlanDesdeCeroRequest request = requestValido();
        request.setFechaFin(null);
        assertTrue(tieneError(request, "La fecha fin es obligatoria"));
    }

    @Test
    void desdePlantillaSinFechaFinFalla() {
        AsignarPlanDesdePlantillaRequest request = new AsignarPlanDesdePlantillaRequest();
        request.setAlumnoId(7L);
        request.setPlantillaId(5L);
        assertTrue(tieneError(request, "La fecha fin es obligatoria"));
        request.setFechaFin(LocalDate.of(2099, 12, 31));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void modificarUnPlanSinFechaFinFalla() {
        PlanRequest cambios = requestValido();
        cambios.setFechaFin(null);
        assertTrue(tieneError(cambios, "La fecha fin es obligatoria"));
    }
}
