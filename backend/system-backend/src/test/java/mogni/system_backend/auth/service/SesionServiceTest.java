package mogni.system_backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.auth.entity.SesionCuenta;
import mogni.system_backend.auth.repository.SesionCuentaRepository;

/** Sesiones largas: plazos por rol, renovación con token nuevo, token reusado y cierre. */
@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Mock private SesionCuentaRepository sesionRepository;

    private final List<SesionCuenta> guardadas = new ArrayList<>();
    private SesionService service;

    @BeforeEach
    void setUp() {
        service = new SesionService(sesionRepository, 45, 14);
        lenient().when(sesionRepository.save(any(SesionCuenta.class))).thenAnswer(inv -> {
            SesionCuenta sesion = inv.getArgument(0);
            if (!guardadas.contains(sesion)) guardadas.add(sesion);
            return sesion;
        });
        lenient().when(sesionRepository.findByTokenHash(anyString())).thenAnswer(inv -> guardadas.stream()
                .filter(sesion -> sesion.getTokenHash().equals(inv.getArgument(0))).findFirst());
        lenient().when(sesionRepository.findByHashAnterior(anyString())).thenAnswer(inv -> guardadas.stream()
                .filter(sesion -> inv.getArgument(0).equals(sesion.getHashAnterior())).findFirst());
    }

    private Cuenta cuenta(Rol rol) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(7L);
        cuenta.setEmail("alguien@mail.com");
        cuenta.setRol(rol);
        cuenta.setActiva(true);
        return cuenta;
    }

    @Test
    void la_sesion_del_alumno_dura_45_dias_y_la_del_personal_14() {
        SesionCuenta alumno = service.iniciar(cuenta(Rol.ALUMNO), AHORA).sesion();
        SesionCuenta propietario = service.iniciar(cuenta(Rol.PROPIETARIO), AHORA).sesion();
        SesionCuenta entrenador = service.iniciar(cuenta(Rol.ENTRENADOR), AHORA).sesion();

        assertEquals(AHORA.plusDays(45), alumno.getVenceEn());
        assertEquals(AHORA.plusDays(14), propietario.getVenceEn());
        assertEquals(AHORA.plusDays(14), entrenador.getVenceEn());
    }

    @Test
    void en_la_base_se_guarda_solo_el_hash_del_token() {
        SesionService.SesionAbierta abierta = service.iniciar(cuenta(Rol.ALUMNO), AHORA);

        assertNotEquals(abierta.token(), abierta.sesion().getTokenHash());
        assertEquals(SesionService.hash(abierta.token()), abierta.sesion().getTokenHash());
    }

    @Test
    void renovar_cambia_el_token_y_vuelve_a_contar_el_plazo_desde_ahora() {
        SesionService.SesionAbierta inicial = service.iniciar(cuenta(Rol.ALUMNO), AHORA);
        LocalDateTime unMesDespues = AHORA.plusDays(30);

        SesionService.SesionAbierta renovada = service.renovar(inicial.token(), unMesDespues);

        assertNotEquals(inicial.token(), renovada.token());
        assertEquals(SesionService.hash(renovada.token()), renovada.sesion().getTokenHash());
        assertEquals(SesionService.hash(inicial.token()), renovada.sesion().getHashAnterior());
        assertEquals(unMesDespues.plusDays(45), renovada.sesion().getVenceEn());
    }

    @Test
    void pasado_el_plazo_sin_usar_la_app_hay_que_volver_a_iniciar_sesion() {
        String token = service.iniciar(cuenta(Rol.ALUMNO), AHORA).token();

        assertThrows(SesionInvalidaException.class, () -> service.renovar(token, AHORA.plusDays(45)));
    }

    @Test
    void reusar_un_token_ya_reemplazado_cierra_la_sesion() {
        String viejo = service.iniciar(cuenta(Rol.ALUMNO), AHORA).token();
        SesionService.SesionAbierta renovada = service.renovar(viejo, AHORA.plusMinutes(20));

        // Alguien copió el token viejo y lo usa una hora después
        assertThrows(SesionInvalidaException.class, () -> service.renovar(viejo, AHORA.plusMinutes(80)));

        assertNotNull(renovada.sesion().getCerradaEn());
        // El dueño legítimo también queda afuera: tiene que volver a iniciar sesión
        assertThrows(SesionInvalidaException.class, () -> service.renovar(renovada.token(), AHORA.plusMinutes(81)));
    }

    @Test
    void dos_renovaciones_casi_simultaneas_no_cierran_la_sesion() {
        String viejo = service.iniciar(cuenta(Rol.ALUMNO), AHORA).token();
        SesionService.SesionAbierta renovada = service.renovar(viejo, AHORA.plusMinutes(20));

        // Otra pestaña renovó con el mismo token unos segundos después
        assertThrows(SesionInvalidaException.class,
                () -> service.renovar(viejo, AHORA.plusMinutes(20).plusSeconds(5)));

        assertNull(renovada.sesion().getCerradaEn());
        assertNotNull(service.renovar(renovada.token(), AHORA.plusMinutes(21)));
    }

    @Test
    void una_sesion_cerrada_no_se_renueva() {
        String token = service.iniciar(cuenta(Rol.ALUMNO), AHORA).token();
        service.cerrar(token);

        assertThrows(SesionInvalidaException.class, () -> service.renovar(token, AHORA.plusMinutes(1)));
    }

    @Test
    void la_cuenta_dada_de_baja_no_renueva() {
        Cuenta cuenta = cuenta(Rol.ALUMNO);
        String token = service.iniciar(cuenta, AHORA).token();
        cuenta.setActiva(false);

        assertThrows(SesionInvalidaException.class, () -> service.renovar(token, AHORA.plusMinutes(1)));
    }

    @Test
    void sin_token_o_con_uno_desconocido_no_renueva() {
        assertThrows(SesionInvalidaException.class, () -> service.renovar(null, AHORA));
        assertThrows(SesionInvalidaException.class, () -> service.renovar("  ", AHORA));
        assertThrows(SesionInvalidaException.class, () -> service.renovar("no-existe", AHORA));
    }

    @Test
    void cerrar_todas_cierra_las_sesiones_de_todos_los_dispositivos() {
        Cuenta cuenta = cuenta(Rol.ALUMNO);
        SesionCuenta celular = service.iniciar(cuenta, AHORA).sesion();
        SesionCuenta compu = service.iniciar(cuenta, AHORA).sesion();
        when(sesionRepository.findByCuentaIdAndCerradaEnIsNull(7L)).thenReturn(List.of(celular, compu));

        service.cerrarTodas(cuenta);

        assertNotNull(celular.getCerradaEn());
        assertNotNull(compu.getCerradaEn());
    }

    @Test
    void la_sesion_vigente_se_consulta_por_id() {
        SesionCuenta sesion = service.iniciar(cuenta(Rol.ALUMNO), AHORA.minusDays(1)).sesion();
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(sesionRepository.findById(2L)).thenReturn(Optional.empty());

        assertEquals(true, service.estaVigente(1L));
        assertEquals(false, service.estaVigente(2L));
        sesion.cerrar(AHORA);
        assertEquals(false, service.estaVigente(1L));
    }
}
