package br.com.bb.cadastro.worker;

import br.com.bb.cadastro.model.OutboxEvent;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OutboxWorker {

    @Inject
    @Channel("pessoa-registrada")
    MutinyEmitter<String> kafkaEmitter;

    @Scheduled(every = "10s")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null").list();
        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {
            CompletableFuture<Void> kafkaAck = new CompletableFuture<>();

            try {
                OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                        .withHeaders(new RecordHeaders()
                                .add("X-Correlation-ID", event.correlationId.getBytes())
                                .add("X-Event-Type", event.type.getBytes()))
                        .build();

                Message<String> message = Message.of(event.payload)
                        .addMetadata(metadata)
                        .withAck(() -> { kafkaAck.complete(null); return CompletableFuture.completedFuture(null); })
                        .withNack(t -> { kafkaAck.completeExceptionally(t); return CompletableFuture.completedFuture(null); });

                kafkaEmitter.send(message);
                kafkaAck.get(10, TimeUnit.SECONDS);

                event.processedAt = LocalDateTime.now();
                event.persist();
                Log.infof("✅ Evento %s (%s) enviado ao Kafka.", event.id, event.type);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.errorf("❌ Thread interrompida durante processamento do Outbox %d: %s", event.id, e.getMessage());
                break;
            } catch (Exception e) {
                Log.errorf("❌ Falha no Outbox %d: %s", event.id, e.getMessage());
            }
        }
    }
}