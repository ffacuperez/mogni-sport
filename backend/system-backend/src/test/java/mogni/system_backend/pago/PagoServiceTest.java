package mogni.system_backend.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mogni.system_backend.alumno.entity.Alumno;
import mogni.system_backend.auth.entity.Cuenta;
import mogni.system_backend.auth.entity.Rol;
import mogni.system_backend.config.ZonaHoraria;
import mogni.system_backend.cuota.entity.Cuota;
import mogni.system_backend.cuota.entity.EstadoCuota;
import mogni.system_backend.cuota.repository.CuotaRepository;
import mogni.system_backend.exception.RecursoNoEncontradoException;
import mogni.system_backend.exception.RegistroInvalidoException;
import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;
import mogni.system_backend.membresia.entity.Membresia;
import mogni.system_backend.pago.dto.PagoResponse;
import mogni.system_backend.pago.dto.RegistrarPagoRequest;
import mogni.system_backend.pago.entity.MedioDePago;
import mogni.system_backend.pago.entity.Pago;
import mogni.system_backend.pago.repository.PagoRepository;
import mogni.system_backend.pago.service.PagoService;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @InjectMocks
    private PagoService pagoService;

    private Alumno alumno;
    private Inscripcion inscripcion;
    private Cuota cuota;

    @BeforeEach
    void setUp() {
        alumno = new Alumno();
        alumno.setId(10L);
        alumno.setCuenta(new Cuenta(1L, "juan@email.com", "hash", Rol.ALUMNO, false, true));
        Membresia membresia = new Membresia(7L, "Pro", 25000.0, 3, null);
        inscripcion = new Inscripcion(5L, LocalDate.now().minusDays(10), null, EstadoInscripcion.ACTIVA, alumno, membresia);
        cuota = Cuota.primeraDe(inscripcion);
        cuota.setId(100L);
    }

    private RegistrarPagoRequest pago(String monto, MedioDePago medio) {
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCuotaId(100L);
        request.setMonto(new BigDecimal(monto));
        request.setMedioDePago(medio);
        return request;
    }

    private void cuotaExiste() {
        when(cuotaRepository.findByIdParaActualizar(100L)).thenReturn(Optional.of(cuota));
    }

    private void pagoSeGuarda() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
    }

    @Test
    void registrarPago_validoDejaLaCuotaPagadaConFechaDeHoy() {
        cuotaExiste();
        pagoSeGuarda();

        PagoResponse response = pagoService.registrarPago(pago("25000", MedioDePago.TRANSFERENCIA));

        assertEquals(EstadoCuota.PAGADA, cuota.getEstado());
        assertEquals(EstadoCuota.PAGADA, response.getEstadoCuota());
        assertEquals(ZonaHoraria.hoy(), response.getFechaPago());
        assertEquals(MedioDePago.TRANSFERENCIA, response.getMedioDePago());
        assertEquals(100L, response.getCuotaId());

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        assertEquals(cuota, captor.getValue().getCuota());
        assertEquals(0, new BigDecimal("25000").compareTo(captor.getValue().getMonto()));
        assertEquals(ZonaHoraria.hoy(), captor.getValue().getFechaPago());
    }

    @Test
    void registrarPago_aceptaElMontoConDecimales() {
        cuotaExiste();
        pagoSeGuarda();

        pagoService.registrarPago(pago("25000.00", MedioDePago.EFECTIVO));

        assertEquals(EstadoCuota.PAGADA, cuota.getEstado());
    }

    @Test
    void registrarPago_sobreCuotaAtrasadaLaDejaPagada() {
        cuota.setEstado(EstadoCuota.ATRASADA);
        cuotaExiste();
        pagoSeGuarda();

        pagoService.registrarPago(pago("25000", MedioDePago.EFECTIVO));

        assertEquals(EstadoCuota.PAGADA, cuota.getEstado());
    }

    @Test
    void registrarPago_rechazaMontoDistintoAlDeLaCuota() {
        cuotaExiste();

        assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago("20000", MedioDePago.EFECTIVO)));
        assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago("30000", MedioDePago.EFECTIVO)));
        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_rechazaCuotaYaPagada() {
        cuota.setEstado(EstadoCuota.PAGADA);
        cuotaExiste();

        assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago("25000", MedioDePago.EFECTIVO)));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_rechazaAlumnoInactivo() {
        alumno.getCuenta().setActiva(false);
        cuotaExiste();

        assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago("25000", MedioDePago.EFECTIVO)));
        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_rechazaCuotaDeInscripcionCancelada() {
        inscripcion.cancelar(LocalDate.now());
        cuotaExiste();

        assertThrows(RegistroInvalidoException.class,
                () -> pagoService.registrarPago(pago("25000", MedioDePago.EFECTIVO)));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_rechazaCuotaInexistente() {
        when(cuotaRepository.findByIdParaActualizar(100L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.registrarPago(pago("25000", MedioDePago.EFECTIVO)));
        verify(pagoRepository, never()).save(any());
    }
}
