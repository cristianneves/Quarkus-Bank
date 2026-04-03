package br.com.bb.notificacoes.messaging;

import br.com.bb.notificacoes.dto.PessoaEventDTO;
import br.com.bb.notificacoes.dto.TransferenciaEventDTO;
import br.com.bb.notificacoes.model.Notificacao;
import br.com.bb.notificacoes.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class NotificacaoConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void setup() {
        Notificacao.deleteAll();
        Usuario.deleteAll();
    }

    @Test
    @DisplayName("Deve processar evento PESSOA_CRIADA")
    void deveProcessarPessoaCriada() throws Exception {
        PessoaEventDTO dto = new PessoaEventDTO();
        dto.keycloakId = "user-1";
        dto.email = "user1@test.com";
        dto.nome = "User One";

        String payload = objectMapper.writeValueAsString(dto);
        enviarMensagemComHeaders("pessoa-registrada", payload, Map.of(
            "X-Event-Type", "PESSOA_CRIADA",
            "X-Correlation-ID", "cid-1"
        ));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                Assertions.assertEquals(1, Usuario.count());
                Assertions.assertEquals(1, Notificacao.count());
                
                Usuario u = Usuario.findById("user-1");
                Assertions.assertNotNull(u);
                Assertions.assertEquals("user1@test.com", u.email);

                Notificacao n = Notificacao.find("keycloakId", "user-1").firstResult();
                Assertions.assertNotNull(n);
                Assertions.assertEquals("BEM_VINDO", n.tipo);
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }

    @Test
    @DisplayName("Deve processar evento PESSOA_EXCLUIDA")
    void deveProcessarPessoaExcluida() throws Exception {
        // Given
        TransactionalRunner.run(() -> {
            Usuario u = new Usuario();
            u.keycloakId = "user-to-delete";
            u.email = "delete@test.com";
            u.nome = "Delete Me";
            u.persist();
        });

        PessoaEventDTO dto = new PessoaEventDTO();
        dto.keycloakId = "user-to-delete";

        String payload = objectMapper.writeValueAsString(dto);
        enviarMensagemComHeaders("pessoa-registrada", payload, Map.of(
            "X-Event-Type", "PESSOA_EXCLUIDA"
        ));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                Assertions.assertEquals(0, Usuario.count());
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }

    @Test
    @DisplayName("Deve processar evento de transferência")
    void deveProcessarTransferencia() throws Exception {
        // Given
        TransactionalRunner.run(() -> {
            Usuario sender = new Usuario();
            sender.keycloakId = "sender-id";
            sender.email = "sender@test.com";
            sender.nome = "Sender";
            sender.persist();

            Usuario receiver = new Usuario();
            receiver.keycloakId = "receiver-id";
            receiver.email = "receiver@test.com";
            receiver.nome = "Receiver";
            receiver.persist();
        });

        TransferenciaEventDTO dto = new TransferenciaEventDTO(
            "123456", "654321", new BigDecimal("100.00"), "idemp-1", "sender@test.com", "receiver@test.com"
        );

        String payload = objectMapper.writeValueAsString(dto);
        enviarMensagemComHeaders("transacoes-bb", payload, Map.of(
            "X-Correlation-ID", "cid-trans-1"
        ));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                Assertions.assertEquals(2, Notificacao.count());
                
                Notificacao nSender = Notificacao.find("tipo", "DEBITO").firstResult();
                Assertions.assertNotNull(nSender);
                Assertions.assertEquals("sender-id", nSender.keycloakId);

                Notificacao nReceiver = Notificacao.find("tipo", "CREDITO").firstResult();
                Assertions.assertNotNull(nReceiver);
                Assertions.assertEquals("receiver-id", nReceiver.keycloakId);
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }

    @Test
    @DisplayName("Não deve processar transferência se usuários não existem")
    void naoDeveProcessarTransferenciaUsuariosNaoExistem() throws Exception {
        TransferenciaEventDTO dto = new TransferenciaEventDTO(
            "123456", "654321", new BigDecimal("100.00"), "idemp-2", "unknown@test.com", "nobody@test.com"
        );

        String payload = objectMapper.writeValueAsString(dto);
        enviarMensagemComHeaders("transacoes-bb", payload, Map.of(
            "X-Correlation-ID", "cid-trans-2"
        ));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                Assertions.assertEquals(0, Notificacao.count());
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }

    private void enviarMensagemComHeaders(String canal, String payload, Map<String, String> headersMap) {
        InMemorySource<Message<String>> source = connector.source(canal);

        RecordHeaders headers = new RecordHeaders();
        headersMap.forEach((k, v) -> headers.add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8))));

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, payload);
        headers.forEach(header -> consumerRecord.headers().add(header));

        IncomingKafkaRecordMetadata<String, String> metadata = new IncomingKafkaRecordMetadata<>(consumerRecord, canal);

        source.send(Message.of(payload).addMetadata(metadata));
    }

    // Helper class to run code in a transaction if needed outside of @Transactional methods
    static class TransactionalRunner {
        @Transactional
        static void run(Runnable r) {
            r.run();
        }
    }
}
