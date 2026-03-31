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
            TestDataFactory.contaPadraoDestino().persist();
        });
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admin")
    @DisplayName("REST: Admin deve listar todas as contas")
    public void adminDeveListarContas() {
        given()
                .when().get("/api/contas")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("REST: Deve retornar 401 ao listar contas sem autenticação")
    public void deveRetornar401AoListarContasSemAuth() {
        given()
                .when().get("/api/contas")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("REST: Dono deve buscar detalhes por KeycloakID")
    public void donoDeveBuscarPorId() {
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
    @DisplayName("REST: Deve negar detalhes para conta de outro usuário")
    public void deveNegarDetalhesParaOutraConta() {
        given()
                .pathParam("keycloakId", "user-destino-id")
                .when().get("/api/contas/detalhes/{keycloakId}")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("REST: Dono deve retornar saldo por KeycloakID")
    public void donoDeveRetornarSaldo() {
        given()
                .pathParam("keycloakId", USER_ID)
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(200)
                .body("saldo", notNullValue());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("REST: Deve negar saldo para conta de outro usuário")
    public void deveNegarSaldoParaOutraConta() {
        given()
                .pathParam("keycloakId", "user-destino-id")
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admin")
    @DisplayName("REST: Admin deve consultar saldo de qualquer conta")
    public void adminDeveConsultarSaldoQualquerConta() {
        given()
                .pathParam("keycloakId", "user-destino-id")
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(200)
                .body("saldo", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admin")
    @DisplayName("REST: Deve retornar 404 para conta inexistente")
    public void deveRetornar404ParaContaInexistente() {
        given()
                .pathParam("keycloakId", "NAO-EXISTE")
                .when().get("/api/contas/saldo/{keycloakId}")
                .then()
                .statusCode(404);
    }
}