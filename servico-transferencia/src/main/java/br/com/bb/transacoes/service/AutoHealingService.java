package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;

@ApplicationScoped
public class AutoHealingService {

    @Inject
    @Channel("transferencias-concluidas")
    Emitter<TransferenciaDTO> emitter;

    // 🔄 Roda a cada 5 minutos (configurável)
    @Scheduled(every = "{recovery.job.interval:5m}", identity = "recovery-job")
    @Transactional
    public void processarPendenciasKafka() {
        Log.info("🔄 [AUTO-HEALING] Iniciando varredura de transferências pendentes...");

        // 1. Busca todas as transferências que o Fallback marcou com erro de envio
        List<Transferencia> pendentes = Transferencia.find("status", "ERRO_ENVIO_KAFKA").list();

        if (pendentes.isEmpty()) {
            Log.info("✅ Nenhuma pendência encontrada. Sistema íntegro.");
            return;
        }

        Log.infof("📦 Encontradas %d transferências para reprocessar.", pendentes.size());

        for (Transferencia t : pendentes) {
            try {
                // 2. Reconstrói o DTO para reenvio
                TransferenciaDTO dto = new TransferenciaDTO(
                        t.numeroOrigem,
                        t.numeroDestino,
                        t.valor,
                        t.idempotencyKey
                );

                // 3. Tenta enviar novamente para o Kafka
                emitter.send(dto);

                // 4. Se o envio não lançou exceção, atualizamos o status para CONCLUIDA
                t.status = "CONCLUIDA";
                Log.infof("✨ Transferência %s recuperada e enviada com sucesso!", t.idempotencyKey);
            } catch (Exception e) {
                // Se falhar de novo, o status continua ERRO_ENVIO_KAFKA para a próxima tentativa
                Log.errorf("❌ Falha ao tentar recuperar %s: %s. Tentaremos novamente no próximo ciclo.",
                        t.idempotencyKey, e.getMessage());
            }
        }
    }
}