package br.com.bb.transacoes.dto;

import java.time.LocalDateTime;

public record ErrorDTO(
        String mensagem,
        int codigo,
        LocalDateTime timestamp
) {}