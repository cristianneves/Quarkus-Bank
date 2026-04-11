package br.com.bb.cadastro.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

@Entity
public class Pessoa extends PanacheEntity {

    @NotBlank(message = "O nome não pode estar em branco")
    public String nome;

    @NotBlank
    @CPF(message = "CPF em formato inválido") // Validação nativa do Hibernate Validator
    @Column(unique = true, nullable = false)
    public String cpf;

    @NotBlank
    @Email(message = "E-mail inválido")
    @Column(unique = true, nullable = false)
    public String email;

    @NotBlank
    @Column(unique = true, nullable = false)
    public String keycloakId; // 🔑 O 'sub' que vem no Token JWT

    @Override
    public String toString() {
        return "Pessoa{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='***.***.***-**'" +
                ", email='[MASKED]'" +
                ", keycloakId='" + keycloakId + '\'' +
                '}';
    }
}