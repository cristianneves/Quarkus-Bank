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
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));

        executarPost(dto).statusCode(201);

        validarSaldo(CONTA_ORIGEM, new BigDecimal("900.00"));
        validarSaldo(CONTA_DESTINO, new BigDecimal("600.50"));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar por saldo insuficiente")
    public void deveFalharPorSaldoInsuficiente() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("5000.00"));
        executarPost(dto).statusCode(422);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharQuandoContaOrigemNaoExiste() {
        TransferenciaDTO dto = criarDTO("99999-9", CONTA_DESTINO, new BigDecimal("10.00"));
        executarPost(dto).statusCode(422);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve falhar ao enviar valor negativo")
    public void deveRetornarErroAoTransferirValorNegativo() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("-50.00"));
        executarPost(dto).statusCode(400);
    }

    @Test
    @DisplayName("Deve retornar 401 ao tentar transferir sem estar autenticado")
    public void deveRetornar401QuandoNaoAutenticado() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10.00"));
        executarPost(dto).statusCode(401);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @TestTransaction
    @DisplayName("Deve retornar 400 quando o valor da transferência for 0")
    public void deveFalharParaValorNaoPositivo() {
        TransferenciaDTO dtoZero = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.ZERO);

        executarPost(dtoZero).statusCode(400);
    }

    // --- MÉTODOS AUXILIARES (HELPERS) ---

    private TransferenciaDTO criarDTO(String origem, String destino, BigDecimal valor) {
        return new TransferenciaDTO(origem, destino, valor, UUID.randomUUID().toString());
    }

    private io.restassured.response.ValidatableResponse executarPost(Object body) {
        return given().body(body).when().post(PATH).then();
    }

    private void validarSaldo(String numeroConta, BigDecimal saldoEsperado) {
        Conta conta = Conta.find("numero", numeroConta).firstResult();
        assertEquals(0, saldoEsperado.compareTo(conta.saldo));
    }
}