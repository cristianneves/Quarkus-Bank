package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class TransferenciaService {

    @Inject
    @Channel("transferencias-concluidas")
    Emitter<TransferenciaDTO> emissorTransferencia;

    @Transactional
    public void realizarTransferencia(TransferenciaDTO dto) {

        Conta origem = Conta.findByNumero(dto.numeroOrigem());
        Conta destino = Conta.findByNumero(dto.numeroDestino());

        if (origem == null || destino == null){
            throw new BusinessException("Conta nao encontrada");
        }
        if (origem.saldo.compareTo(dto.valor()) < 0) {
            throw new BusinessException("Saldo Insuficiente");
        }

        origem.saldo = origem.saldo.subtract(dto.valor());
        destino.saldo = destino.saldo.add(dto.valor());

        emissorTransferencia.send(dto);
    }
}
