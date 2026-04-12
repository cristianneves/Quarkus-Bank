package br.com.bb.cadastro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

public class PessoaDTO {
    
    @NotBlank(message = "O nome não pode estar em branco")
    public String nome;
    
    @NotBlank
    @CPF(message = "CPF em formato inválido")
    public String cpf;
    
    @NotBlank
    @Email(message = "E-mail inválido")
    public String email;
    
    @NotBlank
    public String password;

    @Override
    public String toString() {
        return "PessoaDTO{" +
                "nome='" + nome + '\'' +
                ", cpf='***.***.***-**'" +
                ", email='[MASKED]'" +
                '}';
    }
}