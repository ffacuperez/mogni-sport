package mogni.system_backend.alumno;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.alumno.dto.RegistrarAlumnoRequest;
import mogni.system_backend.pago.entity.MedioDePago;

class RegistrarAlumnoRequestValidationTest {

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

    private RegistrarAlumnoRequest requestValido() {
        RegistrarAlumnoRequest request = new RegistrarAlumnoRequest();
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setDni("12345678");
        request.setEmail("juan@email.com");
        request.setFechaNacimiento(LocalDate.now().minusYears(20));
        request.setTelefono("1122334455");
        request.setSexo("Femenino");
        request.setMembresiaId(1L);
        request.setMedioDePago(MedioDePago.EFECTIVO);
        return request;
    }

    @Test
    void soloObligatoriosEsValido() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void objetivoYAntecedentesSonOpcionales() {
        RegistrarAlumnoRequest request = requestValido();
        request.setObjetivo("Ganar fuerza");
        request.setCuestionarioSalud("Ninguno");
        assertTrue(validator.validate(request).isEmpty());
    }

    // Prueba 3: cada campo obligatorio
    @Test
    void sinNombreFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setNombre(" ");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinApellidoFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setApellido(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinDniFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setDni("");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinFechaNacimientoFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setFechaNacimiento(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinSexoFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setSexo(" ");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinTelefonoFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setTelefono(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinEmailFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setEmail(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    // Prueba 4
    @Test
    void sinMembresiaFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setMembresiaId(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    // MOGNI-73: el alta incluye el pago de la primera cuota
    @Test
    void sinMedioDePagoFalla() {
        RegistrarAlumnoRequest request = requestValido();
        request.setMedioDePago(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    // Prueba 8
    @ParameterizedTest
    @ValueSource(strings = {"juan", "juan@", "@email.com", "juan@email", "juan perez@email.com", "juan@@email.com"})
    void emailInvalidoFalla(String email) {
        RegistrarAlumnoRequest request = requestValido();
        request.setEmail(email);
        assertFalse(validator.validate(request).isEmpty());
    }
}
