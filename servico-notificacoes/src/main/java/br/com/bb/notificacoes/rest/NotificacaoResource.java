package br.com.bb.notificacoes.rest;

import br.com.bb.notificacoes.model.Notificacao;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/notificacoes")
@Produces(MediaType.APPLICATION_JSON)
public class NotificacaoResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("user")
    public List<Notificacao> listarMinhasNotificacoes() {
        String keycloakId = identity.getPrincipal().getName();
        return Notificacao.find("keycloakId = ?1 order by criadoEm desc", keycloakId).list();
    }
}
