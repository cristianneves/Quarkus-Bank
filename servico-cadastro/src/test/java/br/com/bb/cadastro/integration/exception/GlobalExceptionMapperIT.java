package br.com.bb.cadastro.integration.exception;

import br.com.bb.cadastro.integration.base.BaseIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class GlobalExceptionMapperIT extends BaseIntegrationTest {

    @Test
    public void deveRetornar400ParaValidacaoFalha() {
        String jsonInvalido = "{\"nome\":\"\",\"cpf\":\"123\",\"email\":\"invalido\"}";
        
        RestAssured.given()
                .contentType("application/json")
                .body(jsonInvalido)
                .when().post("/api/pessoas/registrar")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}