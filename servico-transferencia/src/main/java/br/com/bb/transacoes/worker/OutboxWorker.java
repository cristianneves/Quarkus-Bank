package br.com.bb.transacoes.worker;

import br.com.bb.transacoes.model.OutboxEvent;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OutboxWorker {

    @Inject
    @Channel("transferencias-concluidas")
    MutinyEmitter<String> kafkaEmitter;

    @Scheduled(every = "10s", identity = "transferencia-outbox-job")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null order by createdAt asc").list();

        if (events.isEmpty()) return;

        Log.infof("🚀 [Outbox] Iniciando processamento de %d eventos de transferência...", events.size());

        for (OutboxEvent event : events) {
            CompletableFuture<Void> kafkaAck = new CompletableFuture<>();

            try {
                org.slf4j.MDC.put("correlationId", event.correlationId);

                // Criar metadados com headers para o Kafka
                OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                        .withHeaders(new RecordHeaders()
                                .add("X-Correlation-ID", event.correlationId.getBytes())
                                .add("X-Event-Type", event.type.getBytes()))
                        .build();

                // Envolver o payload em uma Message com os metadados e lógica de Ack/Nack
                Message<String> message = Message.of(event.payload)
                        .addMetadata(metadata)
                        .withAck(() -> {
                            kafkaAck.complete(null);
                            return CompletableFuture.completedFuture(null);
                        })
                        .withNack(t -> {
                            kafkaAck.completeExceptionally(t);
                            return CompletableFuture.completedFuture(null);
                        });

                kafkaEmitter.send(message);

                // BLOQUEIA por até 10s aguardando o Kafka confirmar
                kafkaAck.get(10, TimeUnit.SECONDS);

                event.processedAt = LocalDateTime.now();
                event.persist();

                Log.infof("✅ [Outbox] Evento %s (%s) enviado ao Kafka.", event.id, event.aggregateId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.errorf("❌ [Outbox] Thread interrompida durante processamento do evento %d: %s", event.id, e.getMessage());
                break;
            } catch (Exception e) {
                Log.errorf("❌ [Outbox] Falha ao enviar evento %s: %s. Re-tentativa no próximo ciclo.",
                        event.id, e.getMessage());
            } finally {
                org.slf4j.MDC.remove("correlationId");
            }
        }
    }
}