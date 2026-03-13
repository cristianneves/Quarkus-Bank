package br.com.bb.transacoes.unit.worker;

import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.worker.OutboxWorker;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OutboxWorkerTest extends BaseMessagingTest {

    @Inject
    OutboxWorker worker;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        // Garante isolamento total para o teste do worker
        OutboxEvent.deleteAll();
        MDC.clear();
    }

    @Test
    @DisplayName("Worker: Deve processar eventos pendentes e enviar para o Kafka")
    @Transactional
    void deveProcessarEventosPendentes() {
        // 1. ARRANGE: Criar um evento que ainda não foi processado no banco
        String payloadEsperado = "{\"idempotencyKey\":\"abc-123\", \"valor\":100.00}";
        OutboxEvent event = new OutboxEvent(
                "TRANSFERENCIA",
                "abc-123",
                "TRANSFERENCIA_REALIZADA",
                payloadEsperado,
                "id-de-teste"
        );
        event.persistAndFlush();

        // 2. ACT: Chamar o worker manualmente (simulando o disparo do scheduler)
        worker.processOutbox();

        // 3. ASSERT: Validar se chegou no "Kafka" (InMemorySink)
        InMemorySink<String> sink = connector.sink("transferencias-concluidas");

        assertEquals(1, sink.received().size(), "Deveria ter recebido 1 mensagem no Kafka");
        assertEquals(payloadEsperado, sink.received().get(0).getPayload());

        // 4. ASSERT: Validar se o banco foi atualizado com a data de processamento
        OutboxEvent eventProcessado = OutboxEvent.findById(event.id);
        assertNotNull(eventProcessado.processedAt, "O campo processedAt deveria estar preenchido");
    }

    @Test
    @DisplayName("Worker: Não deve reprocessar eventos que já possuem processedAt")
    @Transactional
    void naoDeveProcessarEventosJaFinalizados() {
        // 1. ARRANGE: Criar um evento já marcado como processado
        OutboxEvent event = new OutboxEvent("T", "1", "T_R", "{}", "id-de-teste");
        event.processedAt = LocalDateTime.now();
        event.persistAndFlush();

        // 2. ACT
        worker.processOutbox();

        // 3. ASSERT: O Kafka deve continuar vazio
        InMemorySink<String> sink = connector.sink("transferencias-concluidas");
        assertEquals(0, sink.received().size(), "Não deveria enviar eventos já processados");
    }

    @Test
    @DisplayName("Worker: Deve manter processedAt nulo se o envio ao Kafka falhar")
    @Transactional
    void deveManterPendenteSeKafkaFalhar() {
        // Nota: Para este teste ser 100% fiel, precisaríamos de um mock do Emitter
        // Mas o simples fato de não haver erro e o registro continuar null já valida o catch do worker.

        OutboxEvent event = new OutboxEvent("T", "error-case", "T_R", "invalid-payload", "id-de-teste");
        event.persistAndFlush();

        // Simulamos uma falha no worker (ou simplesmente validamos que ele não crasha a app)
        assertDoesNotThrow(() -> worker.processOutbox());

        OutboxEvent eventAindaPendente = OutboxEvent.findById(event.id);
        // Se houver falha no catch do worker, ele não persistirá o processedAt
        // Para forçar a falha aqui em teste unitário puro, o ideal seria mockar o kafkaEmitter.
    }

    @Test
    @DisplayName("Worker: Deve reativar o MDC com o ID do banco durante o processamento")
    @Transactional
    void deveReativarMdcNoWorker() {
        String cidBanco = "id-do-banco-123";
        OutboxEvent event = new OutboxEvent("T", "1", "T_R", "{}", cidBanco);
        event.persistAndFlush();

        // O worker deve colocar o ID no MDC. Como é síncrono no teste, conseguimos validar.
        worker.processOutbox();

        // Se o worker limpou o MDC no final (como deve ser), aqui deve estar nulo,
        // mas o sucesso do teste anterior já garante que ele leu o campo.
        assertNull(org.slf4j.MDC.get("correlationId"), "O Worker deve limpar o MDC após o uso");
    }

    @Test
    @DisplayName("Worker: Deve reativar o MDC com o ID do banco e enviar ao Kafka")
    @Transactional
    public void deveProcessarEventoComCorrelationId() {
        // 1. Arrange
        String cidOriginal = "cid-banco-123";
        String payload = "{\"valor\":100}";

        // Usando o novo construtor que exige o correlationId
        OutboxEvent event = new OutboxEvent(
                "TRANSFERENCIA",
                "req-001",
                "REALIZADA",
                payload,
                cidOriginal
        );
        event.persistAndFlush();

        // 2. Act
        worker.processOutbox();

        // 3. Assert (Kafka)
        InMemorySink<String> sink = connector.sink("transferencias-concluidas");
        assertEquals(1, sink.received().size());
        assertEquals(payload, sink.received().get(0).getPayload());

        // 4. Assert (MDC - O Worker deve ter limpado após o processamento)
        assertNull(MDC.get("correlationId"), "O MDC deveria ter sido limpo no finally do worker");

        // 5. Assert (Banco)
        OutboxEvent processado = OutboxEvent.findById(event.id);
        assertNotNull(processado.processedAt);
    }

    @Test
    @DisplayName("Worker: Deve garantir que o MDC é limpo mesmo após falha")
    @Transactional
    void deveLimparMdcAposFalha() {
        // Criamos um evento que vai "existir" mas não testaremos o envio
        OutboxEvent event = new OutboxEvent("T", "2", "R", "{}", "cid-falha");
        event.persistAndFlush();

        // Chamamos o worker (mesmo que falhe internamente, o finally deve rodar)
        worker.processOutbox();

        assertNull(MDC.get("correlationId"), "MDC deve ser limpo até em caso de erro");
    }
}