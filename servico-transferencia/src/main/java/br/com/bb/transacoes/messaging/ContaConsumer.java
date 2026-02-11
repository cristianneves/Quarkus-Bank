package br.com.bb.transacoes.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import java.math.BigDecimal;
import java.util.Random;

@ApplicationScoped
public class ContaConsumer {

    @Incoming("pessoa-registrada") // Nome do canal no application.properties
    @Transactional
    public void criarContaAoCadastrarPessoa(PessoaEventDTO evento) {
        System.out.println("Recebendo evento de novo cliente: " + evento.nome);

        // Lógica de negócio: Criar conta com saldo zero e número aleatório
        Conta novaConta = new Conta();
        novaConta.keycloakId = evento.keycloakId; // O vínculo oficial entre os mundos
        novaConta.agencia = "0001";
        novaConta.numero = String.valueOf(new Random().nextInt(90000) + 10000);
        novaConta.saldo = BigDecimal.ZERO;

        novaConta.persist(); // Salva no banco db-transferencia (porto 5433)

        System.out.println("Conta " + novaConta.numero + " criada com sucesso para o ID: " + evento.keycloakId);
    }
}