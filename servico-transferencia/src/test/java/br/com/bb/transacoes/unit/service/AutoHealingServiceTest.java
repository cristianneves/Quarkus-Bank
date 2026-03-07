package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.AutoHealingService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel; // 🎯 ESSA É A CERTAimport org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

    @QuarkusTest
    public class AutoHealingServiceTest {

        @Inject
        AutoHealingService autoHealingService;

        @Inject
        @Any
        InMemoryConnector connector;

        Emitter<TransferenciaDTO> mockEmitter = mock(Emitter.class);

    @BeforeEach
    void setup() {
        PanacheMock.mock(Transferencia.class);
        connector.sink("transferencias-concluidas").clear();
    }

    @Test
    @DisplayName("Auto-Healing: Deve reenviar pendências com sucesso")
    void deveRecuperarPendencias() {
        Transferencia t = new Transferencia();
        t.status = "ERRO_ENVIO_KAFKA";
        t.valor = BigDecimal.TEN;
        t.idempotencyKey = "key-123";

        var queryMock = mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA")).thenReturn(queryMock);
        when(queryMock.list()).thenReturn(List.of(t));

        autoHealingService.processarPendenciasKafka();

        Assertions.assertEquals("CONCLUIDA", t.status);
    }

    @Test
    @DisplayName("Auto-Healing: Não deve fazer nada se a lista estiver vazia")
    void deveIgnorarSeNaoHouverPendencias() {
        var queryMock = mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA")).thenReturn(queryMock);
        when(queryMock.list()).thenReturn(List.of());

        autoHealingService.processarPendenciasKafka();

        InMemorySink<Object> sink = connector.sink("transferencias-concluidas");
        Assertions.assertEquals(0, sink.received().size());
    }


}