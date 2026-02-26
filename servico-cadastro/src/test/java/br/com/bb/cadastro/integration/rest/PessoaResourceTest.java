package br.com.bb.cadastro.integration.rest;

import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaResourceTest {

    @InjectMock
    JsonWebToken jwt; // 🔑 Simulamos o Token que o Keycloak enviaria

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @DisplayName("Deve extrair o ID do JWT e salvar a pessoa com sucesso")
    public void deveCadastrarPessoaUsandoSubjectDoToken() {
        String subSimulado = "auth0|123456789";

        // 1. GIVEN: Quando o código pedir o 'sub' do token, entregamos o nosso ID simulado
        when(jwt.getSubject()).thenReturn(subSimulado);

        // 2. GIVEN: Preparamos o Mock do Panache para interceptar o persist()
        PanacheMock.mock(Pessoa.class);
        // Garantimos que o persist não faça nada (não tente ir ao banco real se não houver container)
        Mockito.doAnswer(invocation -> null).when(Mockito.mock(Pessoa.class)).persist();

        // 3. GIVEN: Objeto que o cliente enviaria (Note que NÃO enviamos o keycloakId aqui)
        Pessoa novaPessoa = new Pessoa();
        novaPessoa.nome = "Crislan Sênior";
        novaPessoa.cpf = "024.398.880-01";
        novaPessoa.email = "crislan@bank.com.br";

        // 4. WHEN / THEN: Execução do teste
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(novaPessoa)
                .when().post("/api/pessoas")
                .then()
                .statusCode(201)
                .body("nome", is("Crislan Sênior"))
                .body("keycloakId", is(subSimulado)); // ✅ Validamos se o ID salvo veio do TOKEN
    }
}