package br.com.bb.transacoes.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import java.nio.charset.StandardCharsets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class ContaConsumer {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    ObjectMapper objectMapper;

    @Incoming("pessoa-registrada")
    @Transactional
    @Blocking
    public CompletionStage<Void> criarContaAoCadastrarPessoa(Message<String> mensagem) {

        // 1. Extração do Correlation ID usando IncomingKafkaRecordMetadata
        String correlationId = mensagem.getMetadata(IncomingKafkaRecordMetadata.class)
                .map(metadata -> metadata.getHeaders().lastHeader("X-Correlation-ID"))
                .map(Header::value)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .orElse("cid-auto-" + java.util.UUID.randomUUID());

        // 2. Carimba o Log
        org.slf4j.MDC.put("correlationId", correlationId);

        try {
            // 3. Conversão do JSON para DTO
            PessoaEventDTO evento = objectMapper.readValue(mensagem.getPayload(), PessoaEventDTO.class);

            Log.infof("📩 Processando abertura de conta: %s (ID: %s)", evento.nome(), evento.keycloakId());

            // 4. Idempotência
            if (Conta.count("keycloakId", evento.keycloakId()) > 0) {
                Log.warnf("⚠️ [Idempotência] Conta já existe para o ID: %s", evento.keycloakId());
                return mensagem.ack();
            }

            // 5. Criação da Conta
            Conta novaConta = new Conta();
            novaConta.keycloakId = evento.keycloakId();
            novaConta.agencia = "0001";
            novaConta.nomeTitular = evento.nome();
            novaConta.cpfTitular = evento.cpf();
            novaConta.emailTitular = evento.email();
            novaConta.numero = gerarNumeroContaUnico();
            novaConta.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            novaConta.persistAndFlush();
            Log.infof("✅ Conta %s aberta com sucesso!", novaConta.numero);

            return mensagem.ack();

        } catch (Exception e) {
            Log.errorf("🚨 Erro ao processar evento de conta: %s", e.getMessage());
            return mensagem.nack(e);
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }

    private String gerarNumeroContaUnico() {
        // Simulação de geração
        return String.valueOf(RANDOM.nextInt(900000) + 100000);
    }
}