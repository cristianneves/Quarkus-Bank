package br.com.bb.transacoes.service;

import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Auditoria;
import br.com.bb.transacoes.model.Conta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class AdminService {

    // Ação de Depósito (Administrativo)
    @Transactional
    public void realizarDeposito(DepositoDTO dto) {
        Conta conta = Conta.findByNumeroWithLock(dto.numeroConta());

        if (conta == null) {
            throw new BusinessException("Conta não encontrada para depósito.");
        }

        if (dto.valor().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor do depósito deve ser positivo.");
        }

        conta.saldo = conta.saldo.add(dto.valor());
        // Não precisamos de persist() aqui pois o método é @Transactional
    }

    // Consulta de Auditoria
    public List<Auditoria> consultarLogsPorData(LocalDate data) {
        return Auditoria.find("dataHora >= ?1 and dataHora <= ?2 order by dataHora desc",
                data.atStartOfDay(),
                data.atTime(LocalTime.MAX)).list();
    }
}