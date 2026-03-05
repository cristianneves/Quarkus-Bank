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
        // 1. Log de Recebimento com Rastreabilidade
        Log.infof("📩 Evento recebido - Cliente: %s | ID: %s", evento.nome(), evento.keycloakId());

        // 2. Idempotência via Chave Natural (keycloakId)
        // Usamos o count direto para performance
        if (Conta.count("keycloakId", evento.keycloakId()) > 0) {
            Log.warnf("⚠️ [Idempotência] Ignorando evento duplicado. Conta já existe para o ID: %s", evento.keycloakId());
            return;
        }

        try {
            Conta novaConta = new Conta();
            novaConta.keycloakId = evento.keycloakId();
            novaConta.agencia = "0001";
            novaConta.nomeTitular = evento.nome();
            novaConta.cpfTitular = evento.cpf();
            novaConta.emailTitular = evento.email();

            // Padrão Bancário: Gerar número de conta com dígito ou lógica de negócio
            novaConta.numero = gerarNumeroContaUnico();
            novaConta.saldo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            novaConta.persistAndFlush(); // 🚀 Força o banco a validar as regras AGORA
            Log.infof("✅ Conta %s criada com sucesso...", novaConta.numero);
        } catch (Exception e) {
            // 3. Tratamento de Concorrência Crítica
            // Se outra thread inseriu exatamente no mesmo milissegundo, a Constraint do Banco nos salva
            Log.errorf("🚨 Erro ao criar conta para ID %s: %s", evento.keycloakId(), e.getMessage());
            // Nota: Em um banco real, aqui poderíamos enviar para uma DLQ (Dead Letter Queue)
        }
    }

    private String gerarNumeroContaUnico() {
        // Simulação de geração. No BB, isso viria de uma SEQUENCE do banco para evitar colisões.
        return String.valueOf(RANDOM.nextInt(900000) + 100000);
    }
}