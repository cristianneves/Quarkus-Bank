package br.com.bb.transacoes.unit.worker;

import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.worker.OutboxWorker;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OutboxWorkerTest extends BaseMessagingTest {

    @Inject
    OutboxWorker worker;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        OutboxEvent.deleteAll();
        MDC.clear();
    }

    @Test
    @DisplayName("Worker: Deve processar eventos pendentes e enviar para o Kafka")
    @Transactional
    void deveProcessarEventosPendentes() {
        String payloadEsperado = "{\"idempotencyKey\":\"abc-123\", \"valor\":100.00}";
        OutboxEvent event = new OutboxEvent(
                "TRANSFERENCIA",
                "abc-123",
                "TRANSFERENCIA_REALIZADA",
                payloadEsperado,
                "id-de-teste"
        );
        event.persistAndFlush();

        worker.processOutbox();

        InMemorySink<String> sink = connector.sink("transferencias-concluidas");

        assertEquals(1, sink.received().size(), "Deveria ter recebido 1 mensagem no Kafka");
        assertEquals(payloadEsperado, sink.received().get(0).getPayload());

        OutboxEvent eventProcessado = OutboxEvent.findById(event.id);
        assertNotNull(eventProcessado.processedAt, "O campo processedAt deveria estar preenchido");
    }

    @Test
    @DisplayName("Worker: Não deve reprocessar eventos que já possuem processedAt")
    @Transactional
    void naoDeveProcessarEventosJaFinalizados() {
        OutboxEvent event = new OutboxEvent("T", "1", "T_R", "{}", "id-de-teste");
        event.processedAt = LocalDateTime.now();
        event.persistAndFlush();

        // 2. ACT
        worker.processOutbox();

        InMemorySink<String> sink = connector.sink("transferencias-concluidas");
        assertEquals(0, sink.received().size(), "Não deveria enviar eventos já processados");
    }

    @Test
    @DisplayName("Worker: Deve manter processedAt nulo se o envio ao Kafka falhar")
    @Transactional
    void deveManterPendenteSeKafkaFalhar() {

        OutboxEvent event = new OutboxEvent("T", "error-case", "T_R", "invalid-payload", "id-de-teste");
        event.persistAndFlush();

        assertDoesNotThrow(() -> worker.processOutbox());

        OutboxEvent eventAindaPendente = OutboxEvent.findById(event.id);
    }

    @Test
    @DisplayName("Worker: Deve reativar o MDC com o ID do banco durante o processamento")
    @Transactional
    void deveReativarMdcNoWorker() {
        String cidBanco = "id-do-banco-123";
        OutboxEvent event = new OutboxEvent("T", "1", "T_R", "{}", cidBanco);
        event.persistAndFlush();

        worker.processOutbox();

        assertNull(org.slf4j.MDC.get("correlationId"), "O Worker deve limpar o MDC após o uso");
    }

    @Test
    @DisplayName("Worker: Deve reativar o MDC com o ID do banco e enviar ao Kafka")
    @Transactional
    public void deveProcessarEventoComCorrelationId() {
        String cidOriginal = "cid-banco-123";
        String payload = "{\"valor\":100}";

        OutboxEvent event = new OutboxEvent(
                "TRANSFERENCIA",
                "req-001",
                "REALIZADA",
                payload,
                cidOriginal
        );
        event.persistAndFlush();

        worker.processOutbox();

        InMemorySink<String> sink = connector.sink("transferencias-concluidas");
        assertEquals(1, sink.received().size());
        assertEquals(payload, sink.received().get(0).getPayload());

        assertNull(MDC.get("correlationId"), "O MDC deveria ter sido limpo no finally do worker");

        OutboxEvent processado = OutboxEvent.findById(event.id);
        assertNotNull(processado.processedAt);
    }

    @Test
    @DisplayName("Worker: Deve ignorar se a lista de eventos estiver vazia")
    @Transactional
    void deveIgnorarSeListaVazia() {
        OutboxEvent.deleteAll();

        assertDoesNotThrow(() -> worker.processOutbox());

        InMemorySink<String> sink = connector.sink("transferencias-concluidas");
        assertEquals(0, sink.received().size(), "Não deveria ter enviado nada");
    }

    @Test
    @DisplayName("Worker: Deve limpar MDC após falha")
    @Transactional
    void deveLimparMdcAposFalha() {
        OutboxEvent event = new OutboxEvent("T", "2", "R", "{}", "cid-falha");
        event.persistAndFlush();

        worker.processOutbox();

        assertNull(MDC.get("correlationId"), "MDC deve ser limpo até em caso de erro");
    }
}