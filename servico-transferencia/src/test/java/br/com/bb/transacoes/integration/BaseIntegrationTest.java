package br.com.bb.transacoes.integration;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseIntegrationTest {

    @BeforeAll
    public static void setup() {
        RestAssured.port = 8081;

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Configura JSON global sem perder a referência da porta
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    protected String getCpfFake() {
        return "08140571016";
    }
}