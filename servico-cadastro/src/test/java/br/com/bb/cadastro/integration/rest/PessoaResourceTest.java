package br.com.bb.cadastro.integration.rest;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaResourceTest extends BaseSecurityTest {

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    @Transactional
    void setup() {
        org.slf4j.MDC.clear();
        OutboxEvent.deleteAll();
        Pessoa.deleteAll();
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @DisplayName("Deve extrair o ID do JWT e salvar a pessoa com sucesso no banco real")
    public void deveCadastrarPessoaUsandoSubjectDoToken() {
        String subSimulado = "auth0|123456789";

        // 1. GIVEN: Mock do comportamento do Token
        when(jwt.getSubject()).thenReturn(subSimulado);

        // 2. GIVEN: Objeto enviado pelo cliente
        Pessoa novaPessoa = new Pessoa();
        novaPessoa.nome = "Crislan Sênior";
        novaPessoa.cpf = "024.398.880-01";
        novaPessoa.email = "crislan@bank.com.br";

        // 3. WHEN / THEN: Execução real contra o banco (E2E)
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(novaPessoa)
                .when().post("/api/pessoas")
                .then()
                .statusCode(201)
                .body("nome", is("Crislan Sênior"))
                .body("keycloakId", is(subSimulado));

        // 🛡️ Validação extra de segurança bancária:
        // O registro realmente existe no banco agora?
        Assertions.assertEquals(1, Pessoa.count());
    }

    @Test
    @DisplayName("REST: Deve registrar nova pessoa via DTO com sucesso")
    void deveRegistrarPessoaViaDto() {
        setupKeycloakMockSuccess();
        PessoaDTO dto = criarPessoaDTO();

        RestAssured.given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(dto)
                .when().post("/api/pessoas/registrar") // Ajuste para a rota correta do seu DTO
                .then()
                .statusCode(201)
                .body("nome", is(dto.nome));
    }
}