package br.com.bb.cadastro.worker;

import br.com.bb.cadastro.model.OutboxEvent;
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
    @Channel("pessoa-criada")
    Emitter<String> kafkaEmitter;

    @Scheduled(every = "10s")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null").list();

        if (events.isEmpty()) return;

        Log.infof("Processando %d eventos de outbox...", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaEmitter.send(event.payload)
                        .toCompletableFuture()
                        .get();

                event.processedAt = LocalDateTime.now();
                event.persist();
                Log.infof("Evento %d enviado ao Kafka com sucesso.", event.id);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.errorf("Thread interrompida durante o envio do evento %d", event.id);
                break;

            } catch (Exception e) {
                Log.errorf("Falha ao enviar evento %d: %s", event.id, e.getMessage());
            }
        }
    }
}