package br.com.bb.cadastro.integration.rest;

import br.com.bb.cadastro.client.ContaClient;
import br.com.bb.cadastro.dto.ContaDTO;
import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PerfilResourceTest extends BaseSecurityTest {

    @InjectMock @RestClient ContaClient contaClient;

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @ActivateRequestContext
    @DisplayName("Deve agregar dados de Pessoa (Local) e Conta (Remoto)")
    void deveRetornarPerfilCompleto() {
        String token = "fake-token";
        mockJwtSubject(USER_ID);
        when(jwt.getRawToken()).thenReturn(token);

        QuarkusTransaction.requiringNew().run(() -> {
            Pessoa p = new Pessoa();
            p.nome = "Crislan Sênior";
            p.cpf = CPF_VALIDO;
            p.email = "crislan@bb.com.br";
            p.keycloakId = USER_ID;
            p.persist();
        });

        ContaDTO conta = new ContaDTO();
        conta.numero = "12345-6";
        conta.saldo = new BigDecimal("1500.00");
        when(contaClient.buscarPorKeycloakId(USER_ID, "Bearer " + token)).thenReturn(conta);

        RestAssured.given()
                .when().get("/api/perfil")
                .then()
                .statusCode(200)
                .body("nome", is("Crislan Sênior"))
                .body("numeroConta", is("12345-6"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @ActivateRequestContext
    @DisplayName("Deve retornar 404 quando a pessoa não existe no banco local")
    void deveRetornar404QuandoPessoaNaoLocalizada() {
        mockJwtSubject("id-inexistente");

        // Nenhuma pessoa é persistida aqui

        RestAssured.given()
                .when().get("/api/perfil")
                .then()
                .statusCode(404); // Validando o tratamento de erro que sugerimos no PerfilResource
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @ActivateRequestContext
    @DisplayName("Deve retornar 502 quando o serviço de conta está fora do ar")
    void deveRetornar502QuandoServicoContaFalha() {
        mockJwtSubject(USER_ID);
        when(jwt.getRawToken()).thenReturn("token");

        // Persiste pessoa real
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
            Pessoa p = new Pessoa();
            p.nome = "Crislan"; p.cpf = CPF_VALIDO; p.email = "c@bb.com.br";
            p.keycloakId = USER_ID;
            p.persist();
        });

        // Simula erro no Rest Client (Microprofile Rest Client lança WebApplicationException)
        when(contaClient.buscarPorKeycloakId(any(), any()))
                .thenThrow(new jakarta.ws.rs.WebApplicationException(500));

        RestAssured.given()
                .when().get("/api/perfil")
                .then()
                .statusCode(502);
    }
}