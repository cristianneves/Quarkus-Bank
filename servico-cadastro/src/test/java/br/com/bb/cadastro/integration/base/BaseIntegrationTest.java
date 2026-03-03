package br.com.bb.cadastro.integration.base;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseIntegrationTest {

    protected static final String CPF_VALIDO = "75625688044";
    protected static final String USER_ID = "user-id-123";

    @BeforeEach
    @Transactional
    protected void cleanDB() {
        OutboxEvent.deleteAll();
        Pessoa.deleteAll();
    }

    protected PessoaDTO criarPessoaDTO() {
        PessoaDTO dto = new PessoaDTO();
        dto.nome = "Crislan Sênior";
        dto.cpf = CPF_VALIDO;
        dto.email = "crislan@bb.com.br";
        dto.password = "senha123";
        return dto;
    }
}