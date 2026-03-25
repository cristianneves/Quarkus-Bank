package br.com.bb.transacoes.integration.resource;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class ContaResourceTest extends BaseIntegrationTest {

    @BeforeEach
    public void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Transferencia.deleteAll();
            Conta.deleteAll();
            Conta.getEntityManager().flush();
            TestDataFactory.contaPadraoOrigem().persist();
        });
    }

    @Test
    @DisplayName("REST: Deve listar todas as contas")
    public void deveListarContas() {
        given()
                .when().get("/api/contas")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("REST: Deve buscar detalhes por KeycloakID")
    public void deveBuscarPorId() {
        given()
                .pathParam("keycloakId", USER_ID)
                .when().get("/api/contas/detalhes/{keycloakId}")
                .then()
                .statusCode(200)
                .body("keycloakId", is(USER_ID))
                .body("numero", is(CONTA_ORIGEM));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("REST: Deve retornar saldo por KeycloakID")
    public void deveRetornarSaldo() {
        given()
                .pathParam("keycloakId", USER_ID)
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(200)
                .body("saldo", notNullValue());
    }

    @Test
    @DisplayName("REST: Deve retornar 404 para conta inexistente")
    public void deveRetornar404ParaContaInexistente() {
        given()
                .pathParam("keycloakId", "NAO-EXISTE")
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(404);
    }
}