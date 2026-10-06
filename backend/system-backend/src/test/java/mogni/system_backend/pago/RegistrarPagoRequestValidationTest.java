package mogni.system_backend.pago;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.entity.MedioDePago;

class RegistrarPagoRequestValidationTest {

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

    private RegistrarPagoRequest requestValido() {
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCuotaId(1L);
        request.setMedioDePago(MedioDePago.EFECTIVO);
        return request;
    }

    @Test
    void requestValidoNoTieneErrores() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void sinCuotaFalla() {
        RegistrarPagoRequest request = requestValido();
        request.setCuotaId(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void sinMedioDePagoFalla() {
        RegistrarPagoRequest request = requestValido();
        request.setMedioDePago(null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
