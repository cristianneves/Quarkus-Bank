package br.com.bb.transacoes.integration.base;

import io.quarkus.arc.Arc;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public abstract class BaseMessagingTest extends BaseIntegrationTest {

    @Inject
    @Any
    protected InMemoryConnector connector;

    @BeforeEach
    public void limparCanais() {
        if (connector.sink("transferencias-concluidas") != null) {
            connector.sink("transferencias-concluidas").clear();
        }
    }

    protected <T> void enviarMensagem(String canal, T payload) {
        InMemorySource<T> source = connector.source(canal);
        source.send(payload);
    }

    protected void enviarMensagemComHeaders(String canal, String payload, Map<String, String> headersMap) {
        InMemorySource<Message<String>> source = connector.source(canal);

        RecordHeaders headers = new RecordHeaders();
        headersMap.forEach((k, v) -> headers.add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8))));

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, payload);
        headers.forEach(header -> consumerRecord.headers().add(header));

        IncomingKafkaRecordMetadata<String, String> metadata = new IncomingKafkaRecordMetadata<String, String>(consumerRecord, canal);

        source.send(Message.of(payload).addMetadata(metadata));
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