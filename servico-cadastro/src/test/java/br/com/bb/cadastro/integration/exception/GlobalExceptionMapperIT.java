package br.com.bb.cadastro.integration.exception;

import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class GlobalExceptionMapperIT extends BaseSecurityTest {

    @Test
    public void deveRetornar400ParaValidacaoFalha() {
        String jsonInvalido = "{\"nome\":\"\",\"cpf\":\"123\",\"email\":\"invalido\",\"password\":\"123\"}";
        
        RestAssured.given()
                .contentType("application/json")
                .body(jsonInvalido)
                .when().post("/api/pessoas/registrar")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void deveRetornar409ParaConflitoDeBanco() {
        setupKeycloakMockSuccess();
        
        var dto = criarPessoaDTO();
        dto.email = "conflito@bb.com.br";
        
        RestAssured.given()
                .contentType("application/json")
                .body(dto)
                .when().post("/api/pessoas/registrar")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());
        
        dto = criarPessoaDTO();
        dto.cpf = CPF_VALIDO;
        dto.email = "outro@bb.com.br";
        
        RestAssured.given()
                .contentType("application/json")
                .body(dto)
                .when().post("/api/pessoas/registrar")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
    }
}