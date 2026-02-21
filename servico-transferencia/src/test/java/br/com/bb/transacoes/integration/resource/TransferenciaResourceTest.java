package br.com.bb.transacoes.integration.resource;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TransferenciaResourceTest extends BaseIntegrationTest {

    private static final String PATH = "/api/transferencias";

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve realizar uma transferência com sucesso")
    public void deveRealizarTransferenciaComSucesso() {
        TransferenciaDTO dto = new TransferenciaDTO(
                CONTA_ORIGEM,
                CONTA_DESTINO,
                new BigDecimal("100.00"),
                UUID.randomUUID().toString()
        );

        given()
                .body(dto)
                .when()
                .post(PATH)
                .then()
                .statusCode(201);

        Conta origem = Conta.find("numero", CONTA_ORIGEM).firstResult();
        assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar ao transferir valor maior que o saldo disponível")
    public void deveFalharPorSaldoInsuficiente() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("5000.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post(PATH)
                .then()
                .statusCode(422); // Unprocessable Entity
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharQuandoContaOrigemNaoExiste() {
        TransferenciaDTO dto = new TransferenciaDTO("99999-9", CONTA_DESTINO, new BigDecimal("10.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post(PATH)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar ao enviar valor negativo")
    public void deveRetornarErroAoTransferirValorNegativo() {
        String corpoRequest = """
            {
                "numeroOrigem": "%s",
                "numeroDestino": "%s",
                "valor": -50.00,
                "idempotencyKey": "%s"
            }
            """.formatted(CONTA_ORIGEM, CONTA_DESTINO, UUID.randomUUID().toString());

        given()
                .body(corpoRequest)
                .when()
                .post(PATH)
                .then()
                .statusCode(400); // Bad Request
    }

    @Test
    @DisplayName("Deve retornar 401 ao tentar transferir sem estar autenticado")
    public void deveRetornar401QuandoNaoAutenticado() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post(PATH)
                .then()
                .statusCode(401);
    }
}