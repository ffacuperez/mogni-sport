package mogni.system_backend.alumno;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.alumno.dto.ActualizarAlumnoRequest;

class ActualizarAlumnoRequestValidationTest {

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

    private ActualizarAlumnoRequest requestValido() {
        ActualizarAlumnoRequest request = new ActualizarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12345678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(LocalDate.now().minusYears(20));
        request.setTelefono("1122334455");
        request.setSexo("Masculino");
        return request;
    }

    @Test
    void requestValidoEsValido() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    // El sexo es obligatorio también al modificar: a los alumnos viejos sin sexo se les pide al editarlos
    @Test
    void sinSexoFalla() {
        ActualizarAlumnoRequest request = requestValido();
        request.setSexo(null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
