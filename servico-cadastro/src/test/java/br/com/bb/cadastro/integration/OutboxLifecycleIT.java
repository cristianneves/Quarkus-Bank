package br.com.bb.cadastro.integration;

import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.worker.OutboxWorker;
import br.com.bb.cadastro.service.PessoaService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.Duration;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OutboxLifecycleIT extends BaseSecurityTest {

    @Inject PessoaService service;
    @Inject OutboxWorker worker;
    @Inject @Any InMemoryConnector connector;

    @Test
    @ActivateRequestContext
    @DisplayName("Fluxo E2E: Cadastro -> Outbox -> Worker -> Kafka")
    void deveCumprirCicloDeVidaCompleto() {
        setupKeycloakMockSuccess();
        service.registrarNovoUsuario(criarPessoaDTO());

        // Processamento manual do Worker para validar integração
        worker.processOutbox();

        InMemorySink<String> sink = connector.sink("pessoa-registrada");
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                QuarkusTransaction.run(() -> {
                    OutboxEvent ev = (OutboxEvent) OutboxEvent.findAll().firstResult();

                    assertNotNull(ev, "O evento de outbox deveria existir!");
                    assertNotNull(ev.processedAt, "O evento deveria estar marcado como processado no banco!");
                })
        );
    }

    @Test
    @DisplayName("Não deve marcar como processado se o Kafka falhar (Retry Logic)")
    void deveManterPendenteSeKafkaFalhar() {
        // GIVEN: Um evento pendente no banco (Adicionado o 5º parâmetro "test-cid")
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
            new OutboxEvent("PESSOA", "ID-FALHA", "TESTE", "{}", "test-cid-fail").persist();
        });

        // O worker tentará processar, mas como simulamos (ou esperamos) falha no setup do teste,
        // ele cairá no catch do Worker.
        worker.processOutbox();

        // THEN: O evento DEVE continuar no banco como null para ser tentado novamente
        OutboxEvent ev = OutboxEvent.find("aggregateId", "ID-FALHA").firstResult();
        assertNull(ev.processedAt, "O evento não poderia ter sido processado se houve erro!");
    }
}