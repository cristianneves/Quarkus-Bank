package br.com.bb.transacoes.it;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@QuarkusTest
public class ContaIntegrationTest {

    @Inject
    @Any
    InMemoryConnector connector; // O "simulador" de Kafka

    @Test
    public void deveCriarContaQuandoReceberEventoDoKafka() throws InterruptedException {
        // 1. Criamos um ID aleatório para não chocar com lixo de outros testes
        String keycloakId = UUID.randomUUID().toString();

        // 2. Criamos o objeto que o seu ContaConsumer espera
        PessoaEventDTO evento = new PessoaEventDTO(
                keycloakId,
                "Usuario Teste Pipeline",
                "123.456.789-01",
                "teste@pipeline.com"
        );

        // 3. Injetamos o evento no canal de entrada (Incoming)
        connector.source("pessoa-registrada").send(evento);

        // 4. Esperamos 1 segundo para o banco processar
        Thread.sleep(1000);

        // 5. VALIDAÇÃO PROFISSIONAL: Buscamos direto no banco
        // Fazemos a busca agora para garantir que o Hibernate pegue do DB e não do cache
        Conta contaNoBanco = Conta.find("keycloakId", keycloakId).firstResult();

        // 6. Assertions
        Assertions.assertNotNull(contaNoBanco, "A conta deveria ter sido criada pelo Kafka fake!");
        Assertions.assertEquals(keycloakId, contaNoBanco.keycloakId);
        System.out.println("✅ Sucesso: Conta criada com número: " + contaNoBanco.numero);
    }
}