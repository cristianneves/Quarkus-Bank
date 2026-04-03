package br.com.bb.notificacoes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PessoaEventDTO {
    public String keycloakId;
    public String email;
    public String nome;
}
