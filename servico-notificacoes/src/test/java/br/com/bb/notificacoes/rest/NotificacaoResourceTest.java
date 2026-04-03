package br.com.bb.notificacoes.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class NotificacaoResourceTest {

    @Test
    @DisplayName("Deve retornar 401 ao tentar listar sem autenticação")
    public void testListarSemAuth() {
        given()
          .when().get("/api/notificacoes")
          .then()
             .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = "user")
    @DisplayName("Deve retornar lista vazia para usuário sem notificações")
    public void testListarVazioComAuth() {
        given()
          .when().get("/api/notificacoes")
          .then()
             .statusCode(200)
             .body(is("[]"));
    }
}
