package br.com.bb.transacoes.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PessoaEventDTO {
    public String keycloakId;
    public String nome;
    public String cpf;
    public String email;
}