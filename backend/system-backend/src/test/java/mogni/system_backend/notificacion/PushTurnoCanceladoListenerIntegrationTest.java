package mogni.system_backend.notificacion;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import mogni.system_backend.notificacion.service.PushService;
import mogni.system_backend.turno.service.TurnoCanceladoPorGimnasioEvent;

/**
 * La push de un turno cancelado sale recién cuando se confirma la cancelación, nunca si se revierte, y en otro hilo
 * (el propietario no espera a los servicios de push). Con Spring real: transacciones, eventos y @Async.
 */
@SpringBootTest
@ActiveProfiles("test")
class PushTurnoCanceladoListenerIntegrationTest {

    private static final TurnoCanceladoPorGimnasioEvent EVENTO = new TurnoCanceladoPorGimnasioEvent(
            31L, 7L, "Turno cancelado por el gimnasio", "Se canceló tu turno del 15/10 a las 18:00.");

    @MockitoBean private PushService pushService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void alConfirmarseLaCancelacionMandaLaPushEnOtroHilo() throws Exception {
        CompletableFuture<Thread> hiloDeLaPush = new CompletableFuture<>();
        doAnswer(inv -> {
            hiloDeLaPush.complete(Thread.currentThread());
            return 1;
        }).when(pushService).enviarAviso(any(), any(), any(), any());

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> eventPublisher.publishEvent(EVENTO));

        verify(pushService, timeout(5000)).enviarAviso(31L, "turno-7", EVENTO.titulo(), EVENTO.mensaje());
        assertNotEquals(Thread.currentThread(), hiloDeLaPush.get(5, TimeUnit.SECONDS));
    }

    @Test
    void siLaCancelacionSeRevierteNoMandaNada() {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            eventPublisher.publishEvent(EVENTO);
            estado.setRollbackOnly();
        });

        verify(pushService, after(1000).never()).enviarAviso(any(), any(), any(), any());
    }

    @Test
    void unErrorAlMandarLaPushNoLlegaAQuienCancelo() {
        doThrow(new IllegalStateException("el servicio de push no responde"))
                .when(pushService).enviarAviso(any(), any(), any(), any());

        // Si la excepción llegara hasta acá, el test fallaría
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> eventPublisher.publishEvent(EVENTO));

        verify(pushService, timeout(5000)).enviarAviso(31L, "turno-7", EVENTO.titulo(), EVENTO.mensaje());
    }
}
