package br.com.bb.cadastro.integration.worker;

import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.worker.OutboxWorker;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull; // ✅ JUnit 5


@QuarkusTest
public class OutboxWorkerTest extends BaseSecurityTest {

    @Inject
    OutboxWorker worker;

    @Test
    @DisplayName("Worker: Não deve fazer nada se a lista de eventos estiver vazia")
    void deveIgnorarSeNaoHouverEventos() {
        // O setup limpa o banco na BaseIntegrationTest
        worker.processOutbox();
        // Se não estourou erro e o log foi limpo, cobrimos o 'if empty'
    }

    @Test
    @DisplayName("Worker: Deve marcar como processado após envio com sucesso")
    void deveProcessarEventoComSucesso() {
        QuarkusTransaction.requiringNew().run(() -> {
            // Adicionado "cid-sucesso"
            new OutboxEvent("T", "1", "T", "{}", "cid-sucesso").persist();
        });

        worker.processOutbox();

        QuarkusTransaction.run(() -> {
            OutboxEvent ev = OutboxEvent.findAll().firstResult();
            assertNotNull(ev.processedAt);
        });
    }

    @Test
    @DisplayName("Worker: Deve processar evento pendente e marcar como processado")
    void deveProcessarEventoPendente() {
        QuarkusTransaction.requiringNew().run(() -> {
            // Adicionado "cid-pendente"
            new OutboxEvent("T", "1", "T", "{}", "cid-pendente").persist();
        });

        worker.processOutbox();

        QuarkusTransaction.run(() -> {
            OutboxEvent ev = OutboxEvent.findAll().firstResult();
            assertNotNull(ev.processedAt, "O Worker deveria ter preenchido o processedAt");
        });
    }
}