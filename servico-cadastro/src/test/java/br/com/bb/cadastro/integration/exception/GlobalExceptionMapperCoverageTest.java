package br.com.bb.cadastro.integration.exception;

import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class GlobalExceptionMapperCoverageTest extends BaseSecurityTest {

    @Test
    public void testWebException404() {
        given()
        .when().get("/test-exception/web-exception-404")
        .then()
        .statusCode(404)
        .body(containsString("Recurso não encontrado"));
    }

    @Test
    public void testWebException409() {
        given()
        .when().get("/test-exception/web-exception-409")
        .then()
        .statusCode(409)
        .body(containsString("Conflito"));
    }

    @Test
    public void testWebException500() {
        given()
        .when().get("/test-exception/web-exception-500")
        .then()
        .statusCode(500)
        .body(containsString("Erro interno"));
    }

    @Test
    public void testGenericException() {
        given()
        .when().get("/test-exception/generic-exception")
        .then()
        .statusCode(500)
        .body(containsString("Erro interno no servidor"));
    }

    @Test
    public void testForceValidationException() {
        given()
        .when().get("/test-exception/force-validation-exception")
        .then()
        .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    public void testForceDBException() {
        given()
        .when().get("/test-exception/force-db-exception")
        .then()
        .statusCode(anyOf(is(409), is(500)));
    }
}