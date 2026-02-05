package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class TransferenciaService {

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

    }
}
