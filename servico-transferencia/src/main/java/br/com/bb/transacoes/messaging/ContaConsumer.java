package br.com.bb.transacoes.messaging;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@ApplicationScoped
public class ContaConsumer {

    @Incoming("pessoa-registrada")
    @Transactional
    public void criarContaAoCadastrarPessoa(PessoaEventDTO evento) {
        // 🚀 Acesso via métodos () do Java Record
        Log.infof("Recebendo evento de novo cliente: %s", evento.nome());

        Conta novaConta = new Conta();

        // 🚀 Acesso via métodos () do Java Record
        novaConta.keycloakId = evento.keycloakId();

        novaConta.agencia = "0001";
        novaConta.numero = String.valueOf(new Random().nextInt(90000) + 10000);
        novaConta.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        novaConta.persist();

        Log.infof("Conta %s criada com sucesso para o ID: %s", novaConta.numero, evento.keycloakId());
    }
}