package mogni.system_backend.membresia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mogni.system_backend.membresia.dto.MembresiaRequest;

class MembresiaRequestValidationTest {

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

    private MembresiaRequest requestValido() {
        MembresiaRequest request = new MembresiaRequest();
        request.setNombre("Pro");
        request.setFrecuenciaSemanal(3);
        request.setPrecioActual(25000.0);
        return request;
    }

    @Test
    void requestValidoNoTieneErrores() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rechazaNombreVacio(String nombre) {
        MembresiaRequest request = requestValido();
        request.setNombre(nombre);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rechazaNombreNulo() {
        MembresiaRequest request = requestValido();
        request.setNombre(null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void aceptaDiasEnLosLimites(int dias) {
        MembresiaRequest request = requestValido();
        request.setFrecuenciaSemanal(dias);
        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 7, -1})
    void rechazaDiasFueraDeRango(int dias) {
        MembresiaRequest request = requestValido();
        request.setFrecuenciaSemanal(dias);
        assertFalse(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.01, -100})
    void rechazaPrecioNoPositivo(double precio) {
        MembresiaRequest request = requestValido();
        request.setPrecioActual(precio);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void aceptaPrecioConDecimales() {
        MembresiaRequest request = requestValido();
        request.setPrecioActual(25000.50);
        assertTrue(validator.validate(request).isEmpty());
    }
}
