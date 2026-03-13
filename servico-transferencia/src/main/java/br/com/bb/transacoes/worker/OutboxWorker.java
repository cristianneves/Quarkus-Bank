package br.com.bb.transacoes.worker;

import br.com.bb.transacoes.model.OutboxEvent;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class OutboxWorker {

    @Inject
    @Channel("transferencias-concluidas")
    Emitter<String> kafkaEmitter;

    @Scheduled(every = "10s", identity = "transferencia-outbox-job")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null order by createdAt asc").list();

        if (events.isEmpty()) return;

        Log.infof("🚀 [Outbox] Iniciando processamento de %d eventos de transferência...", events.size());

        for (OutboxEvent event : events) {
            try {
                org.slf4j.MDC.put("correlationId", event.correlationId);

                kafkaEmitter.send(event.payload)
                        .toCompletableFuture()
                        .get();

                event.processedAt = LocalDateTime.now();
                event.persist();

                Log.infof("✅ [Outbox] Evento %s (%s) enviado ao Kafka.", event.id, event.aggregateId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.error("🚨 [Outbox] Thread interrompida durante o processamento.");
                break;
            } catch (Exception e) {
                Log.errorf("❌ [Outbox] Falha ao enviar evento %s: %s. Re-tentativa em 10s.",
                        event.id, e.getMessage());
            } finally {
                org.slf4j.MDC.remove("correlationId");
            }
        }
    }
}