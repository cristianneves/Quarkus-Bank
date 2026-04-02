package br.com.bb.transacoes.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.ContaNumberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class ContaConsumer {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ContaNumberService contaNumberService;

    @Incoming("pessoa-registrada")
    @Transactional
    @Blocking
    public CompletionStage<Void> processarEventoPessoa(Message<String> mensagem) {

        var metadata = mensagem.getMetadata(IncomingKafkaRecordMetadata.class).orElse(null);

        // 1. Extração do Correlation ID e do Tipo do Evento
        String correlationId = extractHeader(metadata, "X-Correlation-ID", "cid-auto-" + java.util.UUID.randomUUID());
        String eventType = extractHeader(metadata, "X-Event-Type", "PESSOA_CRIADA");

        org.slf4j.MDC.put("correlationId", correlationId);

        try {
            PessoaEventDTO evento = objectMapper.readValue(mensagem.getPayload(), PessoaEventDTO.class);

            // 2. Lógica de EXCLUSÃO
            if ("PESSOA_EXCLUIDA".equals(eventType)) {
                Log.warnf("🗑️ [Sincronia] Removendo conta do usuário: %s", evento.keycloakId());
                Conta.delete("keycloakId", evento.keycloakId());
                return mensagem.ack();
            }

            // 3. Lógica de CRIAÇÃO
            Log.infof("📩 Processando abertura de conta: %s", evento.nome());

            if (Conta.count("keycloakId", evento.keycloakId()) > 0) {
                Log.warnf("⚠️ [Idempotência] Conta já existe para o ID: %s", evento.keycloakId());
                return mensagem.ack();
            }

            Conta novaConta = new Conta();
            novaConta.keycloakId = evento.keycloakId();
            novaConta.agencia = "0001";
            novaConta.nomeTitular = evento.nome();
            novaConta.cpfTitular = evento.cpf();
            novaConta.emailTitular = evento.email();
            novaConta.numero = contaNumberService.proximoNumeroConta();
            novaConta.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            novaConta.persistAndFlush();
            Log.infof("✅ Conta %s aberta com sucesso!", novaConta.numero);

            return mensagem.ack();

        } catch (Exception e) {
            Log.errorf("🚨 Erro ao processar evento [%s]: %s", eventType, e.getMessage());
            return mensagem.nack(e);
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }


    private String extractHeader(IncomingKafkaRecordMetadata<?, ?> metadata, String key, String defaultValue) {
        if (metadata == null) return defaultValue;
        return metadata.getHeaders().lastHeader(key) != null
                ? new String(metadata.getHeaders().lastHeader(key).value(), StandardCharsets.UTF_8)
                : defaultValue;
    }
}