package br.com.bb.cadastro.integration;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaServiceTest {

    @Inject
    PessoaService service;

    @InjectMock
    Keycloak keycloak;

    @Inject
    @Any
    InMemoryConnector connector;

    private PessoaDTO dto;

    @BeforeEach
    @jakarta.transaction.Transactional
    void setup() {
        // 🚀 Limpa o banco para evitar o erro de CPF duplicado entre execuções
        Pessoa.deleteAll();

        dto = new PessoaDTO();
        dto.nome = "Crislan Sênior";
        // Usamos um CPF único para garantir que não haja conflito
        dto.cpf = "024.398.880-01";
        dto.email = "crislan@bb.com.br";
        dto.password = "senha123";
    }

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso e enviar para o Kafka")
    public void deveRegistrarComSucesso() {
        InMemorySink<Pessoa> sink = connector.sink("pessoa-criada");
        setupKeycloakMockSuccess();

        // 🚀 Mockamos apenas a busca. O persist deixamos o Panache lidar
        // já que o banco é limpo no setup()
        PanacheQuery queryMock = Mockito.mock(PanacheQuery.class);
        PanacheMock.mock(Pessoa.class);

        when(Pessoa.find(anyString(), (Object[]) any())).thenReturn(queryMock);
        when(queryMock.firstResult()).thenReturn(null);

        Pessoa resultado = service.registrarNovoUsuario(dto);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(1, sink.received().size());
    }

    @Test
    @DisplayName("Deve lançar erro 400 quando o CPF já existe no banco")
    public void deveFalharCpfDuplicado() {
        PanacheQuery queryMock = Mockito.mock(PanacheQuery.class);
        PanacheMock.mock(Pessoa.class);

        when(Pessoa.find(anyString(), (Object[]) any())).thenReturn(queryMock);
        when(queryMock.firstResult()).thenReturn(new Pessoa());

        Assertions.assertThrows(WebApplicationException.class, () -> {
            service.registrarNovoUsuario(dto);
        });
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando o Keycloak falha (Status 500)")
    public void deveFalharQuandoKeycloakEstouraErro() {
        PanacheQuery queryMock = Mockito.mock(PanacheQuery.class);
        PanacheMock.mock(Pessoa.class);
        when(Pessoa.find(anyString(), (Object[]) any())).thenReturn(queryMock);
        when(queryMock.firstResult()).thenReturn(null);

        RealmResource realmResource = Mockito.mock(RealmResource.class);
        UsersResource usersResource = Mockito.mock(UsersResource.class);
        when(keycloak.realm("bank-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(Response.status(500).build());

        Assertions.assertThrows(RuntimeException.class, () -> {
            service.registrarNovoUsuario(dto);
        });
    }

    private void setupKeycloakMockSuccess() {
        RealmResource realmResource = Mockito.mock(RealmResource.class);
        UsersResource usersResource = Mockito.mock(UsersResource.class);
        UserResource userResource = Mockito.mock(UserResource.class);
        RolesResource rolesResource = Mockito.mock(RolesResource.class);
        RoleResource roleResource = Mockito.mock(RoleResource.class);
        RoleMappingResource roleMappingResource = Mockito.mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = Mockito.mock(RoleScopeResource.class);

        when(keycloak.realm("bank-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(usersResource.create(any())).thenReturn(Response.status(201).header("Location", "/user-id-123").build());
        when(usersResource.get("user-id-123")).thenReturn(userResource);
        when(rolesResource.get("user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new org.keycloak.representations.idm.RoleRepresentation());
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }
}