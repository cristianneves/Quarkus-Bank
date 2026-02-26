package br.com.bb.transacoes.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

@ApplicationScoped
public class ContaConsumer {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Incoming("pessoa-registrada")
    @Transactional
    public void criarContaAoCadastrarPessoa(PessoaEventDTO evento) {
        Log.infof("Recebendo evento de novo cliente: %s", evento.nome());

        boolean jaExiste = Conta.find("keycloakId", evento.keycloakId()).count() > 0;

        if (jaExiste) {
            Log.warnf("Conta ignorada: ID %s já possui uma conta ativa.", evento.keycloakId());
            return; // Sai sem criar duplicata
        }

        Conta novaConta = new Conta();
        novaConta.keycloakId = evento.keycloakId();
        novaConta.agencia = "0001";
        novaConta.nomeTitular = evento.nome();
        novaConta.cpfTitular = evento.cpf();
        novaConta.emailTitular = evento.email();
        novaConta.numero = String.valueOf(RANDOM.nextInt(90000) + 10000);
        novaConta.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        novaConta.persist();
        Log.infof("Conta %s criada com sucesso para o ID: %s", novaConta.numero, evento.keycloakId());
    }
}