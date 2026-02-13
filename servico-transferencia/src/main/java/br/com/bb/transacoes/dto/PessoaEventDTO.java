package br.com.bb.transacoes.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PessoaEventDTO(
        String keycloakId,
        String nome,
        String cpf,
        String email // Adicionamos o campo que você incluiu agora
) {}