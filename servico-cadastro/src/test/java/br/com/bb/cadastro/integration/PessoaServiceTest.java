package br.com.bb.cadastro.integration;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PessoaServiceTest {

    @Inject
    PessoaService service;

    @InjectMock
    Keycloak keycloak;

    private PessoaDTO dto;

    @BeforeEach
    @Transactional
    void setup() {
        // 🚨 LIMPEZA REAL: Como não há mais mock do Panache, limpamos as tabelas reais do Flyway
        OutboxEvent.deleteAll();
        Pessoa.deleteAll();

        dto = new PessoaDTO();
        dto.nome = "Crislan Sênior";
        dto.cpf = "024.398.880-01";
        dto.email = "crislan@bb.com.br";
        dto.password = "senha123";
    }

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso e salvar no Outbox")
    public void deveRegistrarComSucesso() {
        setupKeycloakMockSuccess();

        // 🚀 Ação: O service agora persiste no DB e cria o evento de Outbox
        Pessoa resultado = service.registrarNovoUsuario(dto);

        // ✅ Validação de Negócio (Tabela Pessoa)
        assertNotNull(resultado.id);
        assertEquals(1, Pessoa.count());

        // ✅ Validação do Pattern (Tabela Outbox)
        assertEquals(1, OutboxEvent.count());
        OutboxEvent evento = OutboxEvent.findAll().firstResult();

        assertEquals("PESSOA", evento.aggregateType);
        assertEquals(resultado.keycloakId, evento.aggregateId);
        assertEquals("PESSOA_CRIADA", evento.type);
        assertNull(evento.processedAt); // 🛡️ Deve estar pendente para o Worker enviar
        assertTrue(evento.payload.contains(dto.cpf));
    }

    @Test
    @DisplayName("Deve lançar erro 400 quando o CPF já existe no banco (Conflito Real)")
    public void deveFalharCpfDuplicado() {
        // GIVEN: Persistimos uma pessoa real no banco primeiro para causar o conflito
        setupKeycloakMockSuccess();
        service.registrarNovoUsuario(dto);

        // WHEN / THEN: Tentamos registrar o mesmo DTO
        Assertions.assertThrows(WebApplicationException.class, () -> {
            service.registrarNovoUsuario(dto);
        });
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando o Keycloak falha")
    public void deveFalharQuandoKeycloakEstouraErro() {
        RealmResource realmResource = Mockito.mock(RealmResource.class);
        UsersResource usersResource = Mockito.mock(UsersResource.class);

        when(keycloak.realm("bank-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        // Simula erro 500 no Keycloak
        when(usersResource.create(any())).thenReturn(Response.status(500).build());

        Assertions.assertThrows(RuntimeException.class, () -> {
            service.registrarNovoUsuario(dto);
        });

        // 🛡️ Garantia Sênior: Se o Keycloak falhou, nada deve ter sido salvo no banco (Rollback)
        assertEquals(0, Pessoa.count());
        assertEquals(0, OutboxEvent.count());
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