package br.com.bb.transacoes.integration.base;

import br.com.bb.transacoes.base.TestConstants;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.when;

public abstract class BaseIntegrationTest implements TestConstants {

    @InjectMock
    protected JsonWebToken jwt;

    @BeforeAll
    public static void globalSetup() {
        RestAssured.port = 8081;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    @BeforeEach
    public void setupIdentity() {
        when(jwt.getSubject()).thenReturn(USER_ID);
    }

    protected String getCpfFake() {
        return "49721758064";
    }
}