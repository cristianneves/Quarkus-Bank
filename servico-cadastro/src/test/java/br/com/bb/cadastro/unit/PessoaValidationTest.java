package br.com.bb.cadastro.unit;

import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PessoaValidationTest {

    @Inject
    Validator validator;

    @Test
    public void deveFalharComCpfInvalido() {
        Pessoa p = new Pessoa();
        p.nome = "Crislan";
        p.cpf = "123.456.789-00"; // CPF inválido
        p.email = "teste@bb.com.br";
        p.keycloakId = "abc-123";

        var violations = validator.validate(p);
        Assertions.assertFalse(violations.isEmpty());
        Assertions.assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("CPF")));
    }
}