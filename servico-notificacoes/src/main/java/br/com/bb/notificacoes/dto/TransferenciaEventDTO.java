package br.com.bb.notificacoes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferenciaEventDTO(
        String numeroOrigem,
        String numeroDestino,
        BigDecimal valor,
        String idempotencyKey,
        String emailOrigem,
        String emailDestino
) {}
