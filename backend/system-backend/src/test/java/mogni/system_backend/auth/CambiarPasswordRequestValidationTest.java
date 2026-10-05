package mogni.system_backend.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.auth.dto.CambiarPasswordRequest;

class CambiarPasswordRequestValidationTest {

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

    private CambiarPasswordRequest pedido(String actual, String nueva) {
        CambiarPasswordRequest request = new CambiarPasswordRequest();
        request.setPasswordActual(actual);
        request.setPasswordNueva(nueva);
        return request;
    }

    @Test
    void aceptaNuevaDe8Caracteres() {
        assertTrue(validator.validate(pedido("30111222", "12345678")).isEmpty());
    }

    @Test
    void rechazaNuevaDeMenosDe8Caracteres() {
        assertFalse(validator.validate(pedido("30111222", "1234567")).isEmpty());
    }

    @Test
    void rechazaCamposVacios() {
        assertFalse(validator.validate(pedido("", "12345678")).isEmpty());
        assertFalse(validator.validate(pedido("30111222", "")).isEmpty());
    }
}
