package br.com.bb.notificacoes.rest;

import br.com.bb.notificacoes.model.Notificacao;
import br.com.bb.notificacoes.model.Usuario;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class NotificacaoResourceTest {

    @BeforeEach
    @Transactional
    void setup() {
        Notificacao.deleteAll();
        Usuario.deleteAll();
    }

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

    @Test
    @TestSecurity(user = "user-123", roles = "user")
    @DisplayName("Deve retornar lista com notificações do usuário")
    public void testListarComNotificacoes() {
        // Preparar dados no banco
        TransactionalRunner.run(() -> {
            Notificacao n = new Notificacao();
            n.keycloakId = "user-123";
            n.titulo = "Teste";
            n.mensagem = "Mensagem";
            n.tipo = "BEM_VINDO";
            n.status = "ENVIADA";
            n.aggregateId = "agg-1";
            n.persist();
        });

        given()
          .when().get("/api/notificacoes")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].titulo", is("Teste"))
             .body("[0].keycloakId", is("user-123"));
    }

    static class TransactionalRunner {
        @jakarta.transaction.Transactional
        static void run(Runnable r) {
            r.run();
        }
    }
}
