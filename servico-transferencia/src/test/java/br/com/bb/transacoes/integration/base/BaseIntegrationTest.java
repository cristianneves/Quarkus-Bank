package br.com.bb.transacoes.integration.base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

public abstract class BaseIntegrationTest {

    // 1. 🏗️ CONSTANTES GLOBAIS: Centralizadas para todos os testes
    protected static final String USER_ID = "user-origem-id";
    protected static final String CONTA_ORIGEM = "12345-6";
    protected static final String CONTA_DESTINO = "54321-0";
    protected static final BigDecimal SALDO_PADRAO = new BigDecimal("1000.00");

    // 2. 🛡️ SHARED MOCK: Quase todo teste de integração precisará do JWT
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
        // 🚀 Visão Sênior: Por padrão, todo teste assume o usuário logado padrão
        // Isso remove a necessidade de fazer when(jwt.getSubject()) em cada teste
        when(jwt.getSubject()).thenReturn(USER_ID);
    }

    // 3. 🛠️ HELPERS: Métodos utilitários comuns
    protected String getCpfFake() {
        return "49721758064"; // CPF válido para evitar erros de validação
    }
}