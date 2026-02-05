package br.com.bb.transacoes.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaDTO(
        @NotBlank(message = "A conta de origem é obrigatória")
        String numeroOrigem,
        @NotBlank(message = "A conta de destino é obrigatória")
        String numeroDestino,
        @NotNull(message = "O valor não pode ser nulo")
        @Positive(message = "O valor da transferência deve ser maior que zero")
        BigDecimal valor
) {}
