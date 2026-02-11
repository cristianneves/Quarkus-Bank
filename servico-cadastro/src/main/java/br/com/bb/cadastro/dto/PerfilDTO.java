package br.com.bb.cadastro.dto;

import java.math.BigDecimal;

// Em br.com.bb.cadastro.dto.PerfilDTO
public class PerfilDTO {
    public String nome;
    public String cpf;
    public String email;
    public String keycloakId;

    // Dados que virão do outro microserviço
    public String numeroConta;
    public String agencia;
    public BigDecimal saldo;
}