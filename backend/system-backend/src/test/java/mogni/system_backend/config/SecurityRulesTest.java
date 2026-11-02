package mogni.system_backend.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    void soloElAlumnoPuedeVerSusNotificaciones() throws Exception {
        mockMvc.perform(get("/notificaciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notificaciones").with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/notificaciones").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
        // Un ALUMNO pasa la regla de seguridad (404 porque esta cuenta de prueba no tiene alumno asociado)
        mockMvc.perform(get("/notificaciones").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isNotFound());
    }

    @Test
    void plantillasSoloParaPropietarioYEntrenador() throws Exception {
        mockMvc.perform(get("/plantillas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/plantillas").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/plantillas").with(user("n@x.com").roles("NUTRICIONISTA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/plantillas").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isOk());
        mockMvc.perform(get("/plantillas").with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isOk());
    }

    @Test
    void elAlumnoNoAccedeAlCatalogoDeEjercicios() throws Exception {
        mockMvc.perform(get("/ejercicios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ejercicios").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/ejercicios").with(user("a@x.com").roles("ALUMNO"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/ejercicios/1").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        for (String rol : new String[] {"PROPIETARIO", "ENTRENADOR", "NUTRICIONISTA"}) {
            mockMvc.perform(get("/ejercicios").with(user("p@x.com").roles(rol))).andExpect(status().isOk());
        }
    }

    @Test
    void planesAsignadosSoloParaPropietarioYEntrenador() throws Exception {
        String ruta = "/planes-asignados/999999";
        mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ruta).with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(get(ruta).with(user("n@x.com").roles("NUTRICIONISTA"))).andExpect(status().isForbidden());
        // Entrenador y propietario pasan la regla de seguridad (404 porque ese plan no existe)
        mockMvc.perform(get(ruta).with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isNotFound());
        mockMvc.perform(get(ruta).with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isNotFound());
        mockMvc.perform(post("/planes-asignados/desde-cero").with(user("a@x.com").roles("ALUMNO"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void personalDelGimnasioPuedeListar() throws Exception {
        mockMvc.perform(get("/alumnos").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isOk());
        mockMvc.perform(get("/membresias").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isOk());
    }

    @Test
    void cobranzasSoloParaElPropietario() throws Exception {
        mockMvc.perform(get("/cuotas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cuotas").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/cuotas").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/pagos").with(user("e@x.com").roles("ENTRENADOR"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/cuotas").with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isOk());
        // El propietario pasa la regla de seguridad; el cuerpo vacío lo rechaza la validación
        mockMvc.perform(post("/pagos").with(user("p@x.com").roles("PROPIETARIO"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historialDePagosSoloParaElPropietario() throws Exception {
        mockMvc.perform(get("/alumnos/1/pagos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/alumnos/1/pagos").with(user("e@x.com").roles("ENTRENADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alumnos/1/pagos").with(user("a@x.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());
        // El propietario pasa la regla de seguridad; 404 porque este id no existe en la base de prueba
        mockMvc.perform(get("/alumnos/999999/pagos").with(user("p@x.com").roles("PROPIETARIO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void franjasYCupoSoloParaElPropietario() throws Exception {
        for (String ruta : new String[] {"/franjas", "/configuracion-gimnasio"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(ruta).with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
            mockMvc.perform(get(ruta).with(user("n@x.com").roles("NUTRICIONISTA"))).andExpect(status().isForbidden());
            mockMvc.perform(get(ruta).with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
            mockMvc.perform(get(ruta).with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isOk());
        }
        mockMvc.perform(post("/franjas").with(user("e@x.com").roles("ENTRENADOR"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/configuracion-gimnasio/cupo-maximo").with(user("e@x.com").roles("ENTRENADOR"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/franjas/1").with(user("n@x.com").roles("NUTRICIONISTA")))
                .andExpect(status().isForbidden());
        // El propietario pasa la regla de seguridad; el cuerpo vacío lo rechaza la validación
        mockMvc.perform(post("/franjas").with(user("p@x.com").roles("PROPIETARIO"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/configuracion-gimnasio/cupo-maximo").with(user("p@x.com").roles("PROPIETARIO"))
                        .contentType("application/json").content("{\"cupoMaximo\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void turnosDisponiblesSoloParaAlumnos() throws Exception {
        mockMvc.perform(get("/turnos/disponibles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/turnos/disponibles").with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/turnos/disponibles").with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
        // Un ALUMNO pasa la regla de seguridad (404 porque esta cuenta de prueba no tiene alumno asociado)
        mockMvc.perform(get("/turnos/disponibles").with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isNotFound());
    }

    @Test
    void soloElPropietarioPuedeReactivarUnTurnoCancelado() throws Exception {
        String ruta = "/turnos/999999/revertir-cancelacion";
        mockMvc.perform(put(ruta)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(ruta).with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
        mockMvc.perform(put(ruta).with(user("e@x.com").roles("ENTRENADOR"))).andExpect(status().isForbidden());
        // El PROPIETARIO pasa la regla de seguridad (404 porque ese turno no existe)
        mockMvc.perform(put(ruta).with(user("p@x.com").roles("PROPIETARIO"))).andExpect(status().isNotFound());
    }

    @Test
    void agendaSoloParaPropietarioYEntrenador() throws Exception {
        for (String ruta : new String[] {"/agenda/dia", "/agenda/semana"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(ruta).with(user("a@x.com").roles("ALUMNO"))).andExpect(status().isForbidden());
            mockMvc.perform(get(ruta).with(user("n@x.com").roles("NUTRICIONISTA"))).andExpect(status().isForbidden());
        }
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
