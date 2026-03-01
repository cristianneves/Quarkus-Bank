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
}