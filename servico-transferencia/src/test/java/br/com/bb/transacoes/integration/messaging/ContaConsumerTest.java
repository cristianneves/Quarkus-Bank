package br.com.bb.transacoes.integration.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@QuarkusTest
public class ContaConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Test
    @DisplayName("Deve criar uma conta no banco ao receber mensagem do Kafka")
    public void deveProcessarMensagemDeNovoCadastro() {
        // 1. GIVEN: Definimos o Source com o tipo correto do DTO
        InMemorySource<PessoaEventDTO> source = connector.source("pessoa-registrada");

        // 🚀 O construtor do Record agora aceita os 4 campos
        PessoaEventDTO evento = new PessoaEventDTO(
                "user-novo-123",
                "Crislan Sênior",
                "08140571016",
                "crislan@bb.com.br"
        );

        // 2. WHEN: Enviamos o objeto
        source.send(evento);

        // 3. THEN: Validamos a persistência no banco de dados
        Conta contaCriada = Conta.find("keycloakId", "user-novo-123").firstResult();

        Assertions.assertNotNull(contaCriada, "A conta deveria ter sido persistida no banco");

        // Validamos o que realmente existe na sua Entity Conta
        Assertions.assertEquals("user-novo-123", contaCriada.keycloakId, "O ID do Keycloak deve ser o mesmo do evento");
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(contaCriada.saldo),
                "O saldo deve ser numericamente igual a zero");    }
}