package br.com.bb.transacoes.integration.base;

import br.com.bb.transacoes.base.TestConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;

import static org.mockito.Mockito.when;

public abstract class BaseIntegrationTest implements TestConstants {

    @InjectMock
    protected JsonWebToken jwt;

    @TestHTTPResource
    URL url;

    @BeforeEach
    public void setup() {
        RestAssured.port = url.getPort();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        // Mock de identidade padrão
        when(jwt.getSubject()).thenReturn(USER_ID);
    }

    protected String getCpfFake() {
        return "49721758064";
    }
}