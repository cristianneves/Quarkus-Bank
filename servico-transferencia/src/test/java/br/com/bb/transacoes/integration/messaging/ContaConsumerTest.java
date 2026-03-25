package br.com.bb.transacoes.integration.messaging;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class ContaConsumerTest extends BaseMessagingTest {

    private static final String CANAL_ENTRADA = "pessoa-registrada";

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void limparDadosDeTeste() {
        Conta.delete("keycloakId = ?1 OR keycloakId = ?2 OR keycloakId = ?3", "user-id-001", "user-repetido-999", "user-excluir");
    }

    @Test
    @DisplayName("Deve criar uma conta no banco ao receber mensagem do Kafka")
    public void deveProcessarMensagemDeNovoCadastro() throws Exception {
        String id = "user-id-001";
        PessoaEventDTO evento = TestDataFactory.novoEventoPessoa(id, getCpfFake());
        String json = objectMapper.writeValueAsString(evento);
        enviarMensagem(CANAL_ENTRADA, json);

        aguardarProcessamento(() -> {
            Conta conta = Conta.find("keycloakId", id).firstResult();
            Assertions.assertNotNull(conta);
            Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(conta.saldo));
        });
    }

    @Test
    @DisplayName("Não deve criar conta duplicada (Idempotência)")
    public void naoDeveCriarContaDuplicada() throws Exception {
        String id = "user-repetido-999";
        var evento = TestDataFactory.novoEventoPessoa(id, "11122233344");
        String json = objectMapper.writeValueAsString(evento);

        enviarMensagem(CANAL_ENTRADA, json);
        enviarMensagem(CANAL_ENTRADA, json);

        aguardarProcessamento(() -> {
            Assertions.assertEquals(1, Conta.find("keycloakId", id).count());
        });
    }

    @Test
    @DisplayName("Kafka: Deve logar erro ao falhar na persistência (Catch block)")
    public void deveTratarErroNoConsumer() throws Exception {
        var eventoComErro = TestDataFactory.novoEventoPessoa(null, "12345678901");
        String json = objectMapper.writeValueAsString(eventoComErro);

        enviarMensagem(CANAL_ENTRADA, json);

        aguardarProcessamento(() -> {
            Conta conta = Conta.find("cpfTitular", "12345678901").firstResult();
            assertNull(conta, "A conta não deveria existir porque o persist falhou");
        });
    }

    @Test
    @DisplayName("Kafka: Deve remover conta ao receber evento de exclusão")
    public void deveRemoverContaAoExcluirPessoa() throws Exception {
        String id = "user-excluir";

        // GIVEN: Existe uma conta
        QuarkusTransaction.requiringNew().run(() -> {
            Conta c = new Conta();
            c.keycloakId = id;
            c.numero = "998877";
            c.agencia = "0001";
            c.saldo = BigDecimal.ZERO;
            c.cpfTitular = "000.000.000-00";
            c.persist();
        });

        // WHEN: Recebe evento de exclusão
        var evento = TestDataFactory.novoEventoPessoa(id, "000.000.000-00");
        String json = objectMapper.writeValueAsString(evento);

        enviarMensagemComHeaders(CANAL_ENTRADA, json, Map.of("X-Event-Type", "PESSOA_EXCLUIDA"));

        // THEN: Conta deve sumir
        aguardarProcessamento(() -> {
            Assertions.assertEquals(0, Conta.count("keycloakId", id));
        });
    }

    @Test
    @DisplayName("Kafka: Deve processar mensagem sem qualquer metadata (cobertura extractHeader)")
    public void deveProcessarMensagemSemMetadata() throws Exception {
        String id = "user-no-metadata";
        var evento = TestDataFactory.novoEventoPessoa(id, "000.000.000-01");
        String json = objectMapper.writeValueAsString(evento);

        // Envia sem o método 'ComHeaders', logo metadata virá null no consumer
        enviarMensagem(CANAL_ENTRADA, json);

        aguardarProcessamento(() -> {
            Assertions.assertEquals(1, Conta.count("keycloakId", id));
        });
    }
}