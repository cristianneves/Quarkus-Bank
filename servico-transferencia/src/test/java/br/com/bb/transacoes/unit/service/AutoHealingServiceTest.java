package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.AutoHealingService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
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

    @BeforeEach
    void setup() {
        PanacheMock.mock(Transferencia.class);
        connector.sink("transferencias-concluidas").clear();
    }

    @Test
    @DisplayName("Auto-Healing: Deve reenviar pendências e atualizar status")
    void deveRecuperarPendencias() {
        // 1. Criamos uma transferência fictícia com erro
        Transferencia t = new Transferencia();
        t.numeroOrigem = "123";
        t.numeroDestino = "456";
        t.valor = BigDecimal.TEN;
        t.status = "ERRO_ENVIO_KAFKA";
        t.idempotencyKey = "key-123";

        // 2. Mockamos a busca do Panache para retornar essa lista
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA")).thenReturn(mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class));
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA").list()).thenReturn(List.of(t));

        // 3. Chamamos o método manualmente para testar a lógica
        autoHealingService.processarPendenciasKafka();

        // 4. Validações
        // A. Status deve mudar para CONCLUIDA
        Assertions.assertEquals("CONCLUIDA", t.status);

        // B. Verificamos se a mensagem chegou no "Sink" (Kafka em memória)
        InMemorySink<Object> sink = connector.sink("transferencias-concluidas");
        Assertions.assertEquals(1, sink.received().size());
    }

    @Test
    @DisplayName("Auto-Healing: Não deve fazer nada se a lista estiver vazia")
    void deveIgnorarSeNaoHouverPendencias() {
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA")).thenReturn(mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class));
        when(Transferencia.find("status", "ERRO_ENVIO_KAFKA").list()).thenReturn(List.of());

        autoHealingService.processarPendenciasKafka();

        InMemorySink<Object> sink = connector.sink("transferencias-concluidas");
        Assertions.assertEquals(0, sink.received().size());
    }
}