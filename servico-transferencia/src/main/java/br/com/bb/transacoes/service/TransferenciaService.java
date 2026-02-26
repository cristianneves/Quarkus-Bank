package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApplicationScoped
public class TransferenciaService {

    @Inject
    JsonWebToken jwt; // 🔑 A chave da segurança: Identidade de quem chama

    @Inject
    @Channel("transferencias-concluidas")
    Emitter<TransferenciaDTO> emissorTransferencia;

    @Transactional
    public void realizarTransferencia(TransferenciaDTO dto) {
        // Verificação de Idempotência
        Transferencia jaProcessada = Transferencia.findByIdempotencyKey(dto.idempotencyKey());
        if (jaProcessada != null) {
            // Se já existe, apenas ignoramos ou retornamos o recibo.
            // Em bancos, retornar Sucesso (201/200) é a prática para evitar erros no App.
            return;
        }

        // 1. Busca as contas
        Conta origem = Conta.findByNumeroWithLock(dto.numeroOrigem());
        Conta destino = Conta.findByNumeroWithLock(dto.numeroDestino());

        if (origem == null || destino == null) {
            throw new BusinessException("Conta de origem ou destino não encontrada.");
        }

        // 2. VALIDAÇÃO DE SEGURANÇA
        // Verificamos se o 'sub' do Token é igual ao 'keycloakId' da conta de origem
        String callerId = jwt.getSubject();
        if (!origem.keycloakId.equals(callerId)) {
            throw new BusinessException("Você não tem permissão para transferir desta conta.");
        }

        // 3. Validação de Saldo
        if (origem.saldo.compareTo(dto.valor()) < 0) {
            throw new BusinessException("Saldo insuficiente.");
        }

        // 4. Execução Financeira (Débito e Crédito)
        origem.saldo = origem.saldo.subtract(dto.valor());
        destino.saldo = destino.saldo.add(dto.valor());

        // 5. REGISTRO DE AUDITORIA (Persistindo o histórico)
        Transferencia historico = new Transferencia();
        historico.numeroOrigem = dto.numeroOrigem();
        historico.numeroDestino = dto.numeroDestino();
        historico.valor = dto.valor();
        historico.dataHora = LocalDateTime.now();
        historico.status = "CONCLUIDA";
        historico.idempotencyKey = dto.idempotencyKey();
        historico.persist(); // Salva no banco de transações

        // 6. Notificação para o Ecossistema (Kafka)
        emissorTransferencia.send(dto);
    }

    @Transactional
    public void depositar(DepositoDTO dto) {
        Log.infof("Realizando depósito de R$ %s na conta %s", dto.valor(), dto.numeroConta());

        Conta conta = Conta.findByNumeroWithLock(dto.numeroConta());

        if (conta == null) {
            throw new BusinessException("Conta não encontrada para depósito.");
        }

        if (dto.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor do depósito deve ser positivo.");
        }

        conta.saldo = conta.saldo.add(dto.valor());
        conta.persist();

        Log.infof("Depósito realizado com sucesso. Novo saldo da conta %s: R$ %s",
                conta.numero, conta.saldo);
    }
}