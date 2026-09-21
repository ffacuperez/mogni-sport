package mogni.system_backend.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/alumnos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/membresias")).andExpect(status().isUnauthorized());
    }

    @Test
    void alumnoNoPuedeAccederALaGestion() throws Exception {
        mockMvc.perform(get("/alumnos").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/membresias").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
    }

    @Test
    void personalDelGimnasioPuedeListar() throws Exception {
        mockMvc.perform(get("/alumnos").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isOk());
        mockMvc.perform(get("/membresias").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isOk());
    }

    @Test
    void preflightDesdeViteEsPermitidoSinAutenticar() throws Exception {
        mockMvc.perform(options("/alumnos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void origenNoPermitidoEsRechazado() throws Exception {
        mockMvc.perform(options("/alumnos")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
