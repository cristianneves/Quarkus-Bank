package br.com.bb.notificacoes.messaging;

import br.com.bb.notificacoes.dto.PessoaEventDTO;
import br.com.bb.notificacoes.dto.TransferenciaEventDTO;
import br.com.bb.notificacoes.model.Usuario;
import br.com.bb.notificacoes.service.NotificacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class NotificacaoConsumer {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    NotificacaoService notificacaoService;

    @Incoming("pessoa-registrada")
    @Blocking
    @Transactional
    public CompletionStage<Void> consumirPessoaRegistrada(Message<String> msg) {
        try {
            String eventType = getHeader(msg, "X-Event-Type");
            String correlationId = getHeader(msg, "X-Correlation-ID");
            
            Log.infof("📥 Evento recebido em pessoa-registrada: %s (cid: %s)", eventType, correlationId);
            
            if ("PESSOA_CRIADA".equals(eventType)) {
                PessoaEventDTO dto = objectMapper.readValue(msg.getPayload(), PessoaEventDTO.class);
                notificacaoService.atualizarOuCriarUsuario(dto.keycloakId, dto.email, dto.nome);
                
                notificacaoService.registrarNotificacao(
                    dto.keycloakId,
                    "Bem-vindo ao Quarkus Bank!",
                    "Olá " + dto.nome + ", sua conta foi criada com sucesso.",
                    "BEM_VINDO",
                    dto.keycloakId,
                    correlationId
                );
            } else if ("PESSOA_EXCLUIDA".equals(eventType)) {
                PessoaEventDTO dto = objectMapper.readValue(msg.getPayload(), PessoaEventDTO.class);
                notificacaoService.removerUsuario(dto.keycloakId);
            }
            return msg.ack();
        } catch (Exception e) {
            Log.error("❌ Erro ao processar evento de pessoa: " + e.getMessage(), e);
            return msg.nack(e);
        }
    }

    @Incoming("transacoes-bb")
    @Blocking
    @Transactional
    public CompletionStage<Void> consumirTransacao(Message<String> msg) {
        try {
            String correlationId = getHeader(msg, "X-Correlation-ID");
            TransferenciaEventDTO dto = objectMapper.readValue(msg.getPayload(), TransferenciaEventDTO.class);
            
            Log.infof("📥 Transação recebida: %s -> %s (Valor: %s, cid: %s)", 
                    dto.numeroOrigem(), dto.numeroDestino(), dto.valor(), correlationId);

            // Notificar SENDER (DÉBITO)
            Usuario remetente = Usuario.findByEmail(dto.emailOrigem());
            if (remetente != null) {
                notificacaoService.registrarNotificacao(
                    remetente.keycloakId,
                    "Transferência Realizada",
                    String.format("Você transferiu R$ %s para a conta %s.", dto.valor(), dto.numeroDestino()),
                    "DEBITO",
                    dto.idempotencyKey() + "_SENDER",
                    correlationId
                );
            }

            // Notificar RECEIVER (CRÉDITO)
            Usuario destinatario = Usuario.findByEmail(dto.emailDestino());
            if (destinatario != null) {
                notificacaoService.registrarNotificacao(
                    destinatario.keycloakId,
                    "Você recebeu uma transferência!",
                    String.format("Você recebeu R$ %s da conta %s.", dto.valor(), dto.numeroOrigem()),
                    "CREDITO",
                    dto.idempotencyKey() + "_RECEIVER",
                    correlationId
                );
            }
            return msg.ack();
        } catch (Exception e) {
            Log.error("❌ Erro ao processar transação: " + e.getMessage(), e);
            return msg.nack(e);
        }
    }

    private String getHeader(Message<String> msg, String key) {
        return msg.getMetadata(io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata.class)
                .map(metadata -> {
                    org.apache.kafka.common.header.Header header = metadata.getHeaders().lastHeader(key);
                    return header != null ? new String(header.value()) : null;
                })
                .orElse(null);
    }
}
