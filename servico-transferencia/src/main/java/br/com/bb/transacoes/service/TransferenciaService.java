package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.LocalDateTime;

@ApplicationScoped
public class TransferenciaService {

    @Inject
    SecurityIdentity identity;

    @Inject
    @Channel("transferencias-concluidas")
    Emitter<TransferenciaDTO> emissorTransferencia;

    @Transactional
    @Retry(
            maxRetries = 3,
            delay = 1000,
            abortOn = BusinessException.class
    )
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            skipOn = BusinessException.class
    )
    @Fallback(
            fallbackMethod = "fallbackTransferencia",
            skipOn = BusinessException.class
    )
    public void realizarTransferencia(TransferenciaDTO dto) {
        // Idempotência
        Transferencia jaProcessada = Transferencia.findByIdempotencyKey(dto.idempotencyKey());
        if (jaProcessada != null) return;

        Conta origem = Conta.findByNumeroWithLock(dto.numeroOrigem());
        Conta destino = Conta.findByNumeroWithLock(dto.numeroDestino());

        if (origem == null || destino == null) {
            throw new BusinessException("Conta de origem ou destino não encontrada.");
        }

        String callerId = identity.getPrincipal().getName();

        Log.infof("Tentativa de Transferência -> Banco: %s | Token: %s", origem.keycloakId, callerId);

        if (callerId == null || !origem.keycloakId.equals(callerId)) {
            Log.errorf("Bloqueio de Segurança: Conta de %s acessada por %s", origem.keycloakId, callerId);
            throw new BusinessException("Você não tem permissão para transferir desta conta.");
        }

        if (origem.saldo.compareTo(dto.valor()) < 0) {
            throw new BusinessException("Saldo insuficiente.");
        }

        origem.saldo = origem.saldo.subtract(dto.valor());
        destino.saldo = destino.saldo.add(dto.valor());

        Transferencia historico = new Transferencia();
        historico.numeroOrigem = dto.numeroOrigem();
        historico.numeroDestino = dto.numeroDestino();
        historico.valor = dto.valor();
        historico.dataHora = LocalDateTime.now();
        historico.status = "CONCLUIDA";
        historico.idempotencyKey = dto.idempotencyKey();
        historico.persist();

        emissorTransferencia.send(dto);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW) // 🆕 Abre uma nova transação exclusiva para o Fallback
    public void fallbackTransferencia(TransferenciaDTO dto) {
        Log.errorf("🚨 [CONTINGÊNCIA] Falha crítica ao processar transferência: %s", dto.idempotencyKey());

        // 1. Tenta localizar o registro que (provavelmente) foi persistido antes da falha do Kafka
        Transferencia transferencia = Transferencia.findByIdempotencyKey(dto.idempotencyKey());

        if (transferencia != null) {
            // Se o registro existe, apenas atualizamos para um status de atenção
            transferencia.status = "ERRO_ENVIO_KAFKA";
            Log.warnf("📦 Registro %s atualizado para status ERRO_ENVIO_KAFKA para futuro reprocessamento.", dto.idempotencyKey());
        } else {
            // 2. Se a falha foi tão grave que nem o banco salvou na transação original,
            // criamos um registro de 'emergência' aqui.
            Log.warnf("⚠️ Registro não encontrado no banco. Criando entrada de emergência para %s", dto.idempotencyKey());

            Transferencia contingencia = new Transferencia();
            contingencia.numeroOrigem = dto.numeroOrigem();
            contingencia.numeroDestino = dto.numeroDestino();
            contingencia.valor = dto.valor();
            contingencia.idempotencyKey = dto.idempotencyKey();
            contingencia.dataHora = LocalDateTime.now();
            contingencia.status = "FALHA_GRAVE_CONTINGENCIA";
            contingencia.persist();
        }

        // 3. (Opcional) Aqui você poderia disparar um alerta para o time de SRE/Sustentação
        // EnviarEmailAlertas("Falha no Kafka ao processar transferência " + dto.idempotencyKey());
    }
}