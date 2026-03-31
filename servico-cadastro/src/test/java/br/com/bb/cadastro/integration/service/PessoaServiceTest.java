package br.com.bb.cadastro.integration.service;

import br.com.bb.cadastro.client.ContaClient;
import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.integration.base.BaseSecurityTest;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaServiceTest extends BaseSecurityTest {

    @Inject 
    PessoaService service;

    @InjectMock
    @RestClient
    ContaClient contaClient;

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
        RealmResource realmMock = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmMock);
        when(realmMock.roles()).thenThrow(new RuntimeException("Keycloak Down"));

        assertThrows(RuntimeException.class, () -> service.registrarNovoUsuario(criarPessoaDTO()));
    }

    @Test
    @DisplayName("Deve excluir usuário completo quando saldo é zero")
    void deveExcluirUsuarioComSaldoZero() {
        setupKeycloakMockSuccess();
        PessoaDTO dto = criarPessoaDTO();
        dto.email = "excluir@bb.com.br";
        
        service.registrarNovoUsuario(dto);
        
        when(contaClient.obterSaldo(any())).thenReturn(Map.of("saldo", "0.0"));
        
        service.excluirUsuarioCompleto(dto.email);
        
        assertEquals(0, Pessoa.count());
        assertEquals(1, OutboxEvent.count("type", "PESSOA_EXCLUIDA"));
    }

    @Test
    @DisplayName("Deve falhar ao excluir usuário com saldo positivo")
    void deveFalharExcluirComSaldoPositivo() {
        setupKeycloakMockSuccess();
        PessoaDTO dto = criarPessoaDTO();
        dto.email = "comsaldo@bb.com.br";
        
        service.registrarNovoUsuario(dto);
        
        when(contaClient.obterSaldo(any())).thenReturn(Map.of("saldo", "100.50"));
        
        WebApplicationException ex = assertThrows(WebApplicationException.class, 
            () -> service.excluirUsuarioCompleto(dto.email));
        
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Deve abortar exclusão se conta client falhar (fail-closed)")
    void deveAbortarExclusaoSeContaClientFalhar() {
        setupKeycloakMockSuccess();
        PessoaDTO dto = criarPessoaDTO();
        dto.email = "falha@bb.com.br";
        
        service.registrarNovoUsuario(dto);
        
        when(contaClient.obterSaldo(any())).thenThrow(new RuntimeException("Serviço indisponível"));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.excluirUsuarioCompleto(dto.email));

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), ex.getResponse().getStatus());
        assertEquals(1, Pessoa.count());
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar com email já existente")
    void deveFalharEmailDuplicado() {
        setupKeycloakMockSuccess();
        PessoaDTO dto = criarPessoaDTO();
        service.registrarNovoUsuario(dto);
        
        dto.cpf = "99999999999";
        
        WebApplicationException ex = assertThrows(WebApplicationException.class, 
            () -> service.registrarNovoUsuario(dto));
        
        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Deve falhar se Keycloak retornar status diferente de 201")
    void deveFalharQuandoKeycloakRetornaErro() {
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloak.realm("bank-realm")).thenReturn(mock(RealmResource.class));
        when(keycloak.realm("bank-realm").users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(Response.status(400).build());

        assertThrows(RuntimeException.class, () -> service.registrarNovoUsuario(criarPessoaDTO()));
    }

}