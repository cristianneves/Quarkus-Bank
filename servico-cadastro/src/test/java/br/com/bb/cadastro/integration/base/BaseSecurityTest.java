package br.com.bb.cadastro.integration.base;

import io.quarkus.test.InjectMock;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Centraliza o "cascalho" técnico do Keycloak e o mock do JWT.
public abstract class BaseSecurityTest extends BaseIntegrationTest {

    @InjectMock
    protected Keycloak keycloak;

    @InjectMock
    protected JsonWebToken jwt;

    protected void setupKeycloakMockSuccess() {
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
        when(usersResource.create(any())).thenReturn(Response.status(201).header("Location", "/" + USER_ID).build());
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(rolesResource.get("user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new org.keycloak.representations.idm.RoleRepresentation());
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }

    protected void mockJwtSubject(String sub) {
        when(jwt.getSubject()).thenReturn(sub);
    }
}