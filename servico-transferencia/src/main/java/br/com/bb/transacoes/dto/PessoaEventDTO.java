package br.com.bb.transacoes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record PessoaEventDTO(
        String keycloakId,
        String nome,
        String cpf,
        String email
) {}