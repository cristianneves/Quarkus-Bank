package br.com.bb.transacoes.integration.resource;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TransferenciaResourceTest extends BaseIntegrationTest {

    private static final String PATH = "/api/transferencias";

    @BeforeEach
    public void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            Transferencia.deleteAll(); // 🧹 Limpa idempotência para não dar erro se rodar o teste de novo
            Conta.deleteAll();
            TestDataFactory.contaPadraoOrigem().persist();
            TestDataFactory.contaPadraoDestino().persist();
        });
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = {
            @Claim(key = "sub", value = USER_ID) // 🔑 Aqui o framework preenche o jwt.getSubject()
    })
    @DisplayName("Deve realizar uma transferência com sucesso")
    public void deveRealizarTransferenciaComSucesso() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));

        executarPost(dto).statusCode(201);

        validarSaldo(CONTA_ORIGEM, new BigDecimal("900.00"));
        validarSaldo(CONTA_DESTINO, new BigDecimal("600.00"));
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve falhar por saldo insuficiente")
    public void deveFalharPorSaldoInsuficiente() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("5000.00"));
        executarPost(dto).statusCode(422);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharQuandoContaOrigemNaoExiste() {
        TransferenciaDTO dto = criarDTO("999-0", CONTA_DESTINO, new BigDecimal("10.00"));
        executarPost(dto).statusCode(422);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve retornar 400 para valor negativo")
    public void deveFalharParaValorNegativo() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("-10.00"));
        executarPost(dto).statusCode(400);
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    public void deveRetornar401() {
        TransferenciaDTO dto = criarDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10.00"));
        executarPost(dto).statusCode(401);
    }

    // --- AUXILIARES ---
    private TransferenciaDTO criarDTO(String o, String d, BigDecimal v) {
        return new TransferenciaDTO(o, d, v, UUID.randomUUID().toString());
    }

    private io.restassured.response.ValidatableResponse executarPost(Object body) {
        return given().contentType(ContentType.JSON).body(body).when().post(PATH).then();
    }

    private void validarSaldo(String num, BigDecimal esperado) {
        Conta.getEntityManager().clear();
        Conta c = Conta.find("numero", num).firstResult();
        Assertions.assertNotNull(c);
        assertEquals(0, esperado.compareTo(c.saldo));
    }
}