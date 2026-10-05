package mogni.system_backend.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import mogni.system_backend.security.CustomUserDetailsService;

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
    void cambiarPasswordRequiereEstarLogueado() throws Exception {
        mockMvc.perform(post("/auth/cambiar-password").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conCambioDePasswordPendienteNoAccedeALaGestion() throws Exception {
        mockMvc.perform(get("/alumnos").with(user("p@x.com")
                        .authorities(new SimpleGrantedAuthority(CustomUserDetailsService.AUTORIDAD_CAMBIO_PENDIENTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElAlumnoPuedeVerSuPerfil() throws Exception {
        mockMvc.perform(get("/alumnos/me").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/alumnos/me").with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isForbidden());
        // Un ALUMNO pasa la regla de seguridad (404 porque esta cuenta de prueba no tiene alumno asociado)
        mockMvc.perform(get("/alumnos/me").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isNotFound());
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
