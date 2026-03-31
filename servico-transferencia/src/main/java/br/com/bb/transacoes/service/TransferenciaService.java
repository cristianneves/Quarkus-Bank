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

    @Inject
    IdempotencyClaimService idempotencyClaimService;

    /**
     * Executa uma transferência de forma idempotente e segura contra condições de corrida.
     *
     * <p><b>Garantia de atomicidade:</b> A chave de idempotência é reservada via
     * {@code INSERT ... ON CONFLICT DO NOTHING} antes de qualquer operação financeira.
     * Dois threads concorrentes com a mesma chave nunca passarão desta etapa ao mesmo tempo:
     * o PostgreSQL garante que apenas um INSERT terá {@code rowsAffected = 1}; o outro
     * receberá {@code rowsAffected = 0} e retorna imediatamente sem duplicar a transferência.
     * Ao contrário de SELECT → INSERT (check-then-act), esta abordagem não possui janela de
     * corrida.</p>
     *
     * @return {@code true} quando a transferência foi processada agora (HTTP 201),
     *         {@code false} quando a chave já existia (HTTP 200 — resposta idempotente).
     */
    @Transactional
    public boolean realizarTransferencia(TransferenciaDTO dto) {

        // ── 1. Reserva atômica da chave de idempotência ──────────────────────────────
        // Delega ao IdempotencyClaimService (INSERT ... ON CONFLICT DO NOTHING).
        // Lógica de concorrência documentada em IdempotencyClaimService.tryClaimKey().
        if (!idempotencyClaimService.tryClaimKey(dto.idempotencyKey())) {
            return false; // duplicata — caller retorna HTTP 200
        }

        // ── 2. Lock pessimista: exclusividade sobre as contas na transação ────────────
        Conta origem = Conta.findByNumeroWithLock(dto.numeroOrigem());
        Conta destino = Conta.findByNumeroWithLock(dto.numeroDestino());

        if (origem == null || destino == null) {
            // Exceção não verificada → JTA faz rollback, liberando a chave de idempotência
            throw new BusinessException("Conta de origem ou destino não localizada.");
        }

        validarPropriedadeConta(origem);

        // ── 3. Operações financeiras ──────────────────────────────────────────────────
        origem.debitar(dto.valor());
        destino.creditar(dto.valor());

        // ── 4. Completar o registro pré-inserido com os dados da transferência ────────
        // O INSERT da etapa 1 criou uma linha parcial (só id/key/status/dataHora).
        // Agora atualizamos com os dados completos e marcamos como CONCLUIDA.
        Transferencia historico = Transferencia.findByIdempotencyKey(dto.idempotencyKey());
        historico.numeroOrigem  = dto.numeroOrigem();
        historico.numeroDestino = dto.numeroDestino();
        historico.valor         = dto.valor();
        historico.dataHora      = LocalDateTime.now();
        historico.status        = "CONCLUIDA";

        // ── 5. Outbox (evento de domínio) ─────────────────────────────────────────────
        registrarEventoNoOutbox(dto);

        Log.infof("✅ Transferência [%s] processada e evento registrado no Outbox.", dto.idempotencyKey());
        return true; // nova — caller retorna HTTP 201
    }

    private void validarPropriedadeConta(Conta origem) {
        //  PROTEÇÃO BANCÁRIA: Se não há principal ou nome, é uma anomalia de segurança
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


    private void registrarEventoNoOutbox(TransferenciaDTO dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);
            // Recupera o ID que o filtro colocou no MDC
            String correlationId = org.slf4j.MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = "test-" + java.util.UUID.randomUUID();
            }

            OutboxEvent event = new OutboxEvent(
                    "TRANSFERENCIA",
                    dto.idempotencyKey(),
                    "TRANSFERENCIA_REALIZADA",
                    payload,
                    correlationId
            );
            event.correlationId = correlationId; // Salva a "digital" no banco
            event.persist();
        } catch (Exception e) {
            Log.error("❌ Falha crítica ao gerar payload do Outbox", e);
            throw new RuntimeException("Erro interno ao registrar transação.");
        }
    }
}