package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.model.Transferencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class TransferenciaService {

    @Inject
    SecurityIdentity identity;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public void realizarTransferencia(TransferenciaDTO dto) {
        // 1. Idempotência: Se já processamos essa chave, ignoramos silenciosamente
        if (Transferencia.findByIdempotencyKey(dto.idempotencyKey()) != null) {
            Log.warnf("⚠️ Transferência duplicada detectada: %s. Ignorando.", dto.idempotencyKey());
            return;
        }

        // 2. Lock Pessimista: Garante exclusividade sobre as contas durante a transação
        Conta origem = Conta.findByNumeroWithLock(dto.numeroOrigem());
        Conta destino = Conta.findByNumeroWithLock(dto.numeroDestino());

        if (origem == null || destino == null) {
            throw new BusinessException("Conta de origem ou destino não localizada.");
        }

        // 3. Segurança: Valida se o dono do token é o dono da conta de origem
        validarPropriedadeConta(origem);

        // 4. Execução do Negócio (Modelo Rico)
        origem.debitar(dto.valor());
        destino.creditar(dto.valor());

        // 5. Persistência do Histórico
        Transferencia historico = criarHistorico(dto);
        historico.persist();

        // 6. REGISTRO NO OUTBOX (A substituição do envio direto ao Kafka)
        registrarEventoNoOutbox(dto);

        Log.infof("✅ Transferência %s processada e evento registrado no Outbox.", dto.idempotencyKey());
    }

    private void validarPropriedadeConta(Conta origem) {
        // 🛡️ PROTEÇÃO BANCÁRIA: Se não há principal ou nome, é uma anomalia de segurança
        if (identity.getPrincipal() == null || identity.getPrincipal().getName() == null) {
            Log.error("🚨 Falha crítica de segurança: Tentativa de operação sem identificação do Principal.");
            throw new BusinessException("Operação não autorizada: Usuário não identificado.");
        }

        String callerId = identity.getPrincipal().getName();

        if (!origem.keycloakId.equals(callerId)) {
            Log.errorf("🚨 Tentativa de fraude: Usuário %s tentou usar conta de %s", callerId, origem.keycloakId);
            throw new BusinessException("Operação não autorizada para este usuário.");
        }
    }

    private Transferencia criarHistorico(TransferenciaDTO dto) {
        Transferencia t = new Transferencia();
        t.numeroOrigem = dto.numeroOrigem();
        t.numeroDestino = dto.numeroDestino();
        t.valor = dto.valor();
        t.dataHora = LocalDateTime.now();
        t.status = "CONCLUIDA";
        t.idempotencyKey = dto.idempotencyKey();
        return t;
    }

    private void registrarEventoNoOutbox(TransferenciaDTO dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);
            OutboxEvent event = new OutboxEvent(
                    "TRANSFERENCIA",
                    dto.idempotencyKey(),
                    "TRANSFERENCIA_REALIZADA",
                    payload
            );
            event.persist();
        } catch (Exception e) {
            Log.error("❌ Falha crítica ao gerar payload do Outbox", e);
            throw new RuntimeException("Erro interno ao registrar transação.");
        }
    }
}