package br.com.bb.cadastro.integration.service;

import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.RealmResource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaServiceTest extends BaseSecurityTest {

    @Inject PessoaService service;

    @Test
    @DisplayName("Deve persistir Pessoa e OutboxEvent na mesma transação")
    void deveRegistrarComSucesso() {
        setupKeycloakMockSuccess();
        Pessoa resultado = service.registrarNovoUsuario(criarPessoaDTO());

        assertEquals(1, Pessoa.count());
        assertEquals(1, OutboxEvent.count());

        OutboxEvent eventoSalvo = (OutboxEvent) OutboxEvent.findAll().firstResult();

        assertNull(eventoSalvo.processedAt, "O evento deve nascer com processedAt nulo");
    }

    @Test
    @DisplayName("Deve falhar por CPF duplicado")
    void deveFalharCpfDuplicado() {
        setupKeycloakMockSuccess();
        service.registrarNovoUsuario(criarPessoaDTO());
        assertThrows(WebApplicationException.class, () -> service.registrarNovoUsuario(criarPessoaDTO()));
    }

    @Test
    @DisplayName("Deve cadastrar usuário logado e gerar Outbox corretamente")
    void deveCadastrarUsuarioLogadoComOutbox() {
        Pessoa p = new Pessoa();
        p.nome = "Usuario Logado";
        p.cpf = CPF_VALIDO;
        p.email = "logado@bb.com.br";

        Pessoa resultado = service.cadastrarUsuarioLogado(p, "id-keycloak-novo");

        assertNotNull(resultado.id);
        assertEquals("id-keycloak-novo", resultado.keycloakId);

        // Prova que o Outbox foi gerado para este fluxo também
        assertEquals(1, OutboxEvent.count("aggregateId", "id-keycloak-novo"));
    }

    @Test
    @DisplayName("Service: Deve falhar se o Keycloak retornar erro na atribuição de Role")
    void deveFalharSeKeycloakFalharNaRole() {
        // 1. Criamos o mock do "meio do caminho" (o RealmResource)
        RealmResource realmMock = mock(RealmResource.class);

        // 2. Fazemos o Keycloak retornar esse mock em vez de null
        when(keycloak.realm(anyString())).thenReturn(realmMock);

        // 3. Agora o Mockito consegue "chegar" no .roles() para lançar a exceção
        when(realmMock.roles()).thenThrow(new RuntimeException("Keycloak Down"));

        assertThrows(RuntimeException.class, () -> service.registrarNovoUsuario(criarPessoaDTO()));
    }
}