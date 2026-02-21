package br.com.bb.transacoes.integration.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.arc.Arc; // 🚀 Importante para gerenciar o contexto
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class ContaConsumerTest extends BaseIntegrationTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        Conta.delete("keycloakId = ?1 OR keycloakId = ?2", "user-id-001", "user-repetido-999");
    }

    @Test
    @DisplayName("Deve criar uma conta no banco ao receber mensagem do Kafka")
    public void deveProcessarMensagemDeNovoCadastro() {
        InMemorySource<PessoaEventDTO> source = connector.source("pessoa-registrada");
        String novoKeycloakId = "user-id-001";

        source.send(new PessoaEventDTO(novoKeycloakId, "Teste", getCpfFake(), "teste@bb.com.br"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                Conta contaCriada = Conta.find("keycloakId", novoKeycloakId).firstResult();

                Assertions.assertNotNull(contaCriada, "A conta deveria ter sido persistida");
                Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(contaCriada.saldo));
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }

    @Test
    @DisplayName("Não deve criar conta duplicada (Idempotência)")
    public void naoDeveCriarContaDuplicada() {
        InMemorySource<PessoaEventDTO> source = connector.source("pessoa-registrada");
        String keycloakId = "user-repetido-999";
        PessoaEventDTO evento = new PessoaEventDTO(keycloakId, "Teste", getCpfFake(), "teste@bb.com.br");

        source.send(evento);
        source.send(evento);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Arc.container().requestContext().activate();
            try {
                long totalContas = Conta.find("keycloakId", keycloakId).count();
                Assertions.assertEquals(1, totalContas, "O sistema deve ignorar mensagens repetidas");
            } finally {
                Arc.container().requestContext().terminate();
            }
        });
    }
}