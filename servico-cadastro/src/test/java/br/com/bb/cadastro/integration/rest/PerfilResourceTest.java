package br.com.bb.cadastro.integration.rest;

import br.com.bb.cadastro.client.ContaClient;
import br.com.bb.cadastro.dto.ContaDTO;
import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PerfilResourceTest {

    @InjectMock
    JsonWebToken jwt; // Mockamos o Token para simular o usuário logado

    @InjectMock
    @RestClient
    ContaClient contaClient; // Mockamos a chamada para o outro microserviço

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @ActivateRequestContext
    @DisplayName("Deve retornar o perfil completo agregando dados locais e remotos")
    public void deveRetornarPerfilCompleto() {
        String keycloakId = "user-123-abc";
        String tokenRaw = "fake-jwt-token";

        // 1. GIVEN: Mock do JWT
        when(jwt.getSubject()).thenReturn(keycloakId);
        when(jwt.getRawToken()).thenReturn(tokenRaw);

        // 2. GIVEN: Mock do Banco Local (Pessoa)
        Pessoa pessoa = new Pessoa();
        pessoa.nome = "Crislan Sênior";
        pessoa.cpf = "024.398.880-01";
        pessoa.email = "crislan@bb.com.br";

        PanacheQuery queryMock = Mockito.mock(PanacheQuery.class);
        PanacheMock.mock(Pessoa.class);
        when(Pessoa.find("keycloakId", keycloakId)).thenReturn(queryMock);
        when(queryMock.firstResult()).thenReturn(pessoa);

        // 3. GIVEN: Mock do RestClient (Dados da Conta no outro serviço)
        ContaDTO contaMock = new ContaDTO();
        contaMock.numero = "12345-6";
        contaMock.agencia = "0001";
        contaMock.saldo = new BigDecimal("1500.00");

        when(contaClient.buscarPorKeycloakId(keycloakId, "Bearer " + tokenRaw))
                .thenReturn(contaMock);

        // 4. WHEN / THEN: Execução do teste via REST
        RestAssured.given()
                .when().get("/api/perfil")
                .then()
                .statusCode(200)
                .body("nome", is("Crislan Sênior"))
                .body("numeroConta", is("12345-6"))
                .body("saldo", is(1500.0f));
    }
}