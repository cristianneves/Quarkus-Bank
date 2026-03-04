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

    @Scheduled(every = "10s") // Varre o banco a cada 10 segundos
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null").list();

        if (events.isEmpty()) return;

        Log.infof("Processando %d eventos de outbox...", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaEmitter.send(event.payload)
                        .toCompletableFuture()
                        .get(); // 🚀 Pode lançar ExecutionException ou InterruptedException

                event.processedAt = LocalDateTime.now();
                event.persist();
                Log.infof("Evento %d enviado ao Kafka com sucesso.", event.id);

            } catch (InterruptedException e) {
                // 🔴 Regra de Ouro: Restaura o status de interrupção da Thread
                Thread.currentThread().interrupt();
                Log.errorf("Thread interrompida durante o envio do evento %d", event.id);
                break; // Interrompe o loop, já que a thread deve parar

            } catch (Exception e) {
                // Trata outros erros (como falha de conexão no Kafka)
                Log.errorf("Falha ao enviar evento %d: %s", event.id, e.getMessage());
            }
        }
    }
}