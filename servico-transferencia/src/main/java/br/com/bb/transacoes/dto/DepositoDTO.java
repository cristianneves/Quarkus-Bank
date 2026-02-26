package br.com.bb.transacoes.dto;

import java.math.BigDecimal;

public record DepositoDTO(
        String numeroConta,
        BigDecimal valor
) {}