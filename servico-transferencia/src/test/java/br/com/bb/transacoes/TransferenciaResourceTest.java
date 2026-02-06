package br.com.bb.transacoes;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.model.Conta;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TransferenciaResourceTest {

    @Test
    @DisplayName("Deve falhar ao realizar transferencia com um valor negativo")
    public void deveRetornarErroAoTransferirValorNegativo() {
        String corpoRequest = """
            {
                "numeroOrigem": "12345-6",
                "numeroDestino": "54321-0",
                "valor": -50.00
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(corpoRequest)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(400); // Bad Request por causa da validação
    }

    @Test
    @DisplayName("Deve realizar uma transferência entre duas contas com sucesso")
    public void deveRealizarTransferenciaComSucesso() {
        // 1. Preparar os dados (Baseado no seu import.sql)
        // Conta Origem (12345-6) tem 1000.00
        // Conta Destino (54321-0) tem 500.50
        TransferenciaDTO dto = new TransferenciaDTO("12345-6", "54321-0", new BigDecimal("100.00"));

        // 2. Executar a chamada à API
        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/contas/transferir")
                .then()
                .statusCode(200);

        // 3. Validação de "Ponta a Ponta"
        // Buscamos as entidades direto do banco de dados para validar o saldo final
        Conta origem = Conta.findByNumero("12345-6");
        Conta destino = Conta.findByNumero("54321-0");

        // Asserções (Comparando os saldos pós-transferência)
        // 1000.00 - 100.00 = 900.00
        assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo), "O saldo da conta de origem deve ser 900.00");

        // 500.50 + 100.00 = 600.50
        assertEquals(0, new BigDecimal("600.50").compareTo(destino.saldo), "O saldo da conta de destino deve ser 600.50");
    }
}