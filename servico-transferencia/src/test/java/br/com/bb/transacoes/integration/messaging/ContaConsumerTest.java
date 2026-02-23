package br.com.bb.transacoes.integration.messaging;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

@QuarkusTest
public class ContaConsumerTest extends BaseMessagingTest {

    private static final String CANAL_ENTRADA = "pessoa-registrada";

    @BeforeEach
    @Transactional
    void limparDadosDeTeste() {
        Conta.delete("keycloakId = ?1 OR keycloakId = ?2", "user-id-001", "user-repetido-999");
    }

    @Test
    @DisplayName("Deve criar uma conta no banco ao receber mensagem do Kafka")
    public void deveProcessarMensagemDeNovoCadastro() {
        String id = "user-id-001";

        enviarMensagem(CANAL_ENTRADA, TestDataFactory.novoEventoPessoa(id, getCpfFake()));

        aguardarProcessamento(() -> {
            Conta conta = Conta.find("keycloakId", id).firstResult();
            Assertions.assertNotNull(conta);
            Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(conta.saldo));
        });
    }

    @Test
    @DisplayName("Não deve criar conta duplicada (Idempotência)")
    public void naoDeveCriarContaDuplicada() {
        String id = "user-repetido-999";
        var evento = TestDataFactory.novoEventoPessoa(id, "11122233344");

        enviarMensagem(CANAL_ENTRADA, evento);
        enviarMensagem(CANAL_ENTRADA, evento);

        aguardarProcessamento(() -> {
            Assertions.assertEquals(1, Conta.find("keycloakId", id).count());
        });
    }
}