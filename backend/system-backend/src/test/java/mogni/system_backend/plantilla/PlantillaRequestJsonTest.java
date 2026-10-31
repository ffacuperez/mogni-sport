package mogni.system_backend.plantilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import mogni.system_backend.plantilla.dto.EjercicioSesionRequest;
import mogni.system_backend.plantilla.dto.PlantillaRequest;
import mogni.system_backend.plantilla.entity.NivelPlantilla;
import mogni.system_backend.plantilla.entity.ObjetivoPlantilla;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** HU-265: lo que se rechaza al leer el JSON, antes de las validaciones (el controller responde 400). */
class PlantillaRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private void rechaza(String json, Class<?> tipo) {
        assertThrows(JacksonException.class, () -> mapper.readValue(json, tipo), json);
    }

    @Test
    void series_repeticiones_y_rir_aceptan_enteros() {
        EjercicioSesionRequest e = mapper.readValue(
                "{\"ejercicioId\":1,\"series\":4,\"repeticiones\":10,\"rirObjetivo\":0}", EjercicioSesionRequest.class);

        assertEquals(4, e.getSeries());
        assertEquals(10, e.getRepeticiones());
        assertEquals(0, e.getRirObjetivo());
    }

    @Test
    void rir_nulo_sigue_siendo_opcional() {
        assertNull(mapper.readValue("{\"rirObjetivo\":null}", EjercicioSesionRequest.class).getRirObjetivo());
    }

    @Test
    void decimales_se_rechazan_en_vez_de_recortarse() {
        rechaza("{\"series\":2.9}", EjercicioSesionRequest.class);
        rechaza("{\"repeticiones\":8.5}", EjercicioSesionRequest.class);
        rechaza("{\"rirObjetivo\":2.5}", EjercicioSesionRequest.class);
    }

    @Test
    void textos_y_rangos_se_rechazan() {
        rechaza("{\"repeticiones\":\"8-10\"}", EjercicioSesionRequest.class);
        rechaza("{\"repeticiones\":\"10\"}", EjercicioSesionRequest.class);
    }

    @Test
    void nivel_y_objetivo_solo_por_nombre() {
        PlantillaRequest r = mapper.readValue("{\"nivel\":\"AVANZADO\",\"objetivo\":\"PERDIDA_DE_GRASA\"}", PlantillaRequest.class);
        assertEquals(NivelPlantilla.AVANZADO, r.getNivel());
        assertEquals(ObjetivoPlantilla.PERDIDA_DE_GRASA, r.getObjetivo());

        rechaza("{\"nivel\":1}", PlantillaRequest.class);
        rechaza("{\"objetivo\":1}", PlantillaRequest.class);
        rechaza("{\"nivel\":\"EXPERTO\"}", PlantillaRequest.class);
    }
}
