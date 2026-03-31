package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.model.Conta;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/contas")
@Produces(MediaType.APPLICATION_JSON)
public class ContaResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("admin")
    public List<Conta> listarTodas(){
        return Conta.listAll();
    }

    @GET
    @Path("/detalhes/{keycloakId}")
    @RolesAllowed({"user", "admin"})
    public Conta buscarPorId(@PathParam("keycloakId") String keycloakId) {
        validarAcessoConta(keycloakId);
        return Conta.find("keycloakId", keycloakId).firstResult();
    }

    @GET
    @Path("/saldo/{keycloakId}")
    @RolesAllowed({"user", "admin"})
    public Response buscarSaldo(@PathParam("keycloakId") String keycloakId) {
        validarAcessoConta(keycloakId);
        return Conta.find("keycloakId", keycloakId).firstResultOptional()
                .map(conta -> Response.ok(Map.of("saldo", ((Conta) conta).saldo)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    private void validarAcessoConta(String keycloakId) {
        if (identity.hasRole("admin")) {
            return;
        }

        String callerId = identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
        if (callerId == null || !callerId.equals(keycloakId)) {
            throw new ForbiddenException("Acesso negado para esta conta.");
        }
    }
}
