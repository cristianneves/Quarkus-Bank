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
    @Channel("transferencias-concluidas") // Nome do canal no seu application.properties
    Emitter<String> kafkaEmitter;

    // No BB, a latência deve ser baixa. 5 ou 10 segundos é um bom começo para dev.
    @Scheduled(every = "10s", identity = "transferencia-outbox-job")
    @Transactional
    public void processOutbox() {
        // 1. Busca eventos não processados (ordenados pelo mais antigo para manter a ordem cronológica)
        List<OutboxEvent> events = OutboxEvent.find("processedAt is null order by createdAt asc").list();

        if (events.isEmpty()) return;

        Log.infof("🚀 [Outbox] Iniciando processamento de %d eventos de transferência...", events.size());

        for (OutboxEvent event : events) {
            try {
                // 2. Envio síncrono para garantir que o Kafka recebeu antes de marcar como processado
                // O .toCompletableFuture().get() espera a confirmação (ACK) do Broker do Kafka
                kafkaEmitter.send(event.payload)
                        .toCompletableFuture()
                        .get();

                // 3. Sucesso: Marcamos o evento para não ser reenviado
                event.processedAt = LocalDateTime.now();
                event.persist();

                Log.infof("✅ [Outbox] Evento %s (%s) enviado ao Kafka.", event.id, event.aggregateId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Boa prática: restaura o status de interrupção
                Log.error("🚨 [Outbox] Thread interrompida durante o processamento.");
                break;
            } catch (Exception e) {
                // 4. Falha: O evento continua com processedAt = null e será tentado no próximo ciclo
                Log.errorf("❌ [Outbox] Falha ao enviar evento %s: %s. Re-tentativa em 10s.",
                        event.id, e.getMessage());
                // IMPORTANTE: Em um banco, aqui poderíamos implementar um contador de tentativas
                // para mover para uma tabela de 'failed_events' após X erros.
            }
        }
    }
}