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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TransferenciaResourceTest extends BaseIntegrationTest {

    @Test
    @TestSecurity(user = "crislan", roles = "user")
    @TestTransaction
    @DisplayName("Deve realizar uma transferência com sucesso")
    public void deveRealizarTransferenciaComSucesso() {
        TransferenciaDTO dto = new TransferenciaDTO("12345-6", "54321-0", new BigDecimal("100.00"), UUID.randomUUID().toString());

        given()
                .body(dto) // 👈 Não precisa mais de .contentType(JSON), já está no Base!
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(200);

        // Validação de Integridade no Banco
        Conta origem = Conta.findByNumero("12345-6");
        Conta destino = Conta.findByNumero("54321-0");

        assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
        assertEquals(0, new BigDecimal("600.50").compareTo(destino.saldo));
    }

    @Test
    @TestSecurity(user = "crislan", roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar ao transferir valor maior que o saldo disponível")
    public void deveFalharPorSaldoInsuficiente() {
        // Tentando transferir 5000.00 de uma conta que tem 1000.00 (conforme import.sql)
        TransferenciaDTO dto = new TransferenciaDTO("12345-6", "54321-0", new BigDecimal("5000.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(422) // Unprocessable Entity (Regra de Negócio)
                .body("mensagem", is("Saldo Insuficiente"));
    }

    @Test
    @TestSecurity(user = "crislan", roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharQuandoContaOrigemNaoExiste() {
        TransferenciaDTO dto = new TransferenciaDTO("99999-9", "54321-0", new BigDecimal("10.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(422)
                .body("mensagem", is("Conta nao encontrada"));
    }

    @Test
    @TestSecurity(user = "crislan", roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar ao enviar valor negativo")
    public void deveRetornarErroAoTransferirValorNegativo() {
        String corpoRequest = """
            {
                "numeroOrigem": "12345-6",
                "numeroDestino": "54321-0",
                "valor": -50.00
            }
            """;

        given()
                .body(corpoRequest)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(400); // Bad Request (Bean Validation)
    }

    @Test
    @DisplayName("Deve retornar 401 ao tentar transferir sem estar autenticado")
    public void deveRetornar401QuandoNaoAutenticado() {
        TransferenciaDTO dto = new TransferenciaDTO("12345-6", "54321-0", new BigDecimal("10.00"), UUID.randomUUID().toString());

        given()
                .body(dto)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(401); // Sem @TestSecurity deve barrar o acesso
    }
}