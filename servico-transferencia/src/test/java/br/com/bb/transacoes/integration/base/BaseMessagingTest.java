package br.com.bb.transacoes.integration.base;

import io.quarkus.arc.Arc;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public abstract class BaseMessagingTest extends BaseIntegrationTest {

    @Inject
    @Any
    protected InMemoryConnector connector;

    @BeforeEach
    public void limparCanais() {
        // 🚀 Visão Sênior: Limpa os sinks conhecidos antes de cada teste
        // Se você tiver outros canais de saída, adicione-os aqui
        if (connector.sink("transferencias-concluidas") != null) {
            connector.sink("transferencias-concluidas").clear();
        }
    }

    protected <T> void enviarMensagem(String canal, T payload) {
        InMemorySource<T> source = connector.source(canal);
        source.send(payload);
    }

    // Abstrai o boilerplate de contexto e espera assíncrona
    protected void aguardarProcessamento(Runnable assertivas) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                assertivas.run();
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }
}