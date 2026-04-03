package br.com.bb.notificacoes.service;

import br.com.bb.notificacoes.model.Notificacao;
import br.com.bb.notificacoes.model.Usuario;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
public class NotificacaoServiceTest {

    @Inject
    NotificacaoService notificacaoService;

    @org.junit.jupiter.api.BeforeEach
    @jakarta.transaction.Transactional
    void setup() {
        Notificacao.deleteAll();
        Usuario.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("Deve registrar uma nova notificação")
    public void testRegistrarNotificacao() {
        notificacaoService.registrarNotificacao(
            "user-123",
            "Teste",
            "Mensagem de teste",
            "BEM_VINDO",
            "agg-123",
            "cid-123"
        );

        List<Notificacao> notificacoes = Notificacao.find("keycloakId", "user-123").list();
        Assertions.assertEquals(1, notificacoes.size());
        Notificacao n = notificacoes.get(0);
        Assertions.assertEquals("Teste", n.titulo);
        Assertions.assertEquals("ENVIADA", n.status);
        Assertions.assertNotNull(n.enviadoEm);
    }

    @Test
    @TestTransaction
    @DisplayName("Deve criar um novo usuário se não existir")
    public void testAtualizarOuCriarUsuarioNovo() {
        notificacaoService.atualizarOuCriarUsuario("new-user", "new@example.com", "New User");

        Usuario usuario = Usuario.findById("new-user");
        Assertions.assertNotNull(usuario);
        Assertions.assertEquals("new@example.com", usuario.email);
        Assertions.assertEquals("New User", usuario.nome);
    }

    @Test
    @TestTransaction
    @DisplayName("Deve atualizar um usuário existente")
    public void testAtualizarOuCriarUsuarioExistente() {
        Usuario existing = new Usuario();
        existing.keycloakId = "old-user";
        existing.email = "old@example.com";
        existing.nome = "Old User";
        existing.persist();

        notificacaoService.atualizarOuCriarUsuario("old-user", "updated@example.com", "Updated User");

        Usuario updated = Usuario.findById("old-user");
        Assertions.assertNotNull(updated);
        Assertions.assertEquals("updated@example.com", updated.email);
        Assertions.assertEquals("Updated User", updated.nome);
    }

    @Test
    @TestTransaction
    @DisplayName("Deve remover usuário antigo se email for associado a novo keycloakId")
    public void testAtualizarOuCriarUsuarioEmailConflito() {
        Usuario existing = new Usuario();
        existing.keycloakId = "user-a";
        existing.email = "common@example.com";
        existing.nome = "User A";
        existing.persist();
        existing.flush();

        notificacaoService.atualizarOuCriarUsuario("user-b", "common@example.com", "User B");

        Assertions.assertNull(Usuario.findById("user-a"));
        Usuario newUser = Usuario.findById("user-b");
        Assertions.assertNotNull(newUser);
        Assertions.assertEquals("common@example.com", newUser.email);
    }

    @Test
    @TestTransaction
    @DisplayName("Deve remover um usuário")
    public void testRemoverUsuario() {
        Usuario existing = new Usuario();
        existing.keycloakId = "to-be-removed";
        existing.email = "removed@example.com";
        existing.nome = "To Be Removed";
        existing.persist();

        notificacaoService.removerUsuario("to-be-removed");

        Assertions.assertNull(Usuario.findById("to-be-removed"));
    }
}
