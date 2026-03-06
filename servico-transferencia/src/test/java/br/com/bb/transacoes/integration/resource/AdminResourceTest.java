package br.com.bb.transacoes.integration.resource;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Auditoria;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AdminResourceTest extends BaseIntegrationTest {

    @BeforeEach
    public void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Auditoria.deleteAll();
            Conta.deleteAll();
            Conta.getEntityManager().flush();
            TestDataFactory.contaPadraoOrigem().persist();
        });
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admin")
    @DisplayName("Admin: Deve realizar depósito e gerar auditoria de sucesso")
    public void deveDepositarEGerarAuditoria() {
        DepositoDTO dto = new DepositoDTO(CONTA_ORIGEM, new BigDecimal("500.00"));

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when().post("/api/admin/deposito")
                .then().statusCode(200);

        // Valida Saldo
        Conta c = Conta.find("numero", CONTA_ORIGEM).firstResult();
        assertEquals(0, new BigDecimal("1500.00").compareTo(c.saldo));

        // Valida Interceptor (Auditoria)
        List<Auditoria> logs = Auditoria.listAll();
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).detalhes.contains("SUCESSO"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admin")
    @DisplayName("Admin: Deve gerar auditoria de FALHA ao tentar depositar em conta inexistente")
    public void deveGerarAuditoriaMesmoComFalha() {
        DepositoDTO dto = new DepositoDTO("999-9", new BigDecimal("100.00"));

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when().post("/api/admin/deposito")
                .then().statusCode(422); // BusinessException

        // O segredo do interceptor: Mesmo com 422, o log deve existir!
        List<Auditoria> logs = Auditoria.listAll();
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).detalhes.contains("FALHA"));
    }

    @Test
    @TestSecurity(user = "user-comum", roles = "user")
    @DisplayName("Admin: Deve proibir acesso de usuário não-admin")
    public void deveProibirAcesso() {
        DepositoDTO dto = new DepositoDTO(CONTA_ORIGEM, BigDecimal.TEN);

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when().post("/api/admin/deposito")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @DisplayName("Admin: Deve listar logs usando a data atual quando o parâmetro for nulo")
    void deveListarLogsSemParametroData() {
        given()
                .when().get("/api/admin/auditoria")
                .then()
                .statusCode(200);
        // Isso garante que o 'else' do ternário (LocalDate.now()) seja executado
    }
}