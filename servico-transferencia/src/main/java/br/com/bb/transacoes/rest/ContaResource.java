package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import jakarta.inject.Inject;
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
    TransferenciaService service;

    @GET
    public List<Conta> listarTodas(){
        return Conta.listAll();
    }

    @GET
    @Path("/detalhes/{keycloakId}")
    public Conta buscarPorId(@PathParam("keycloakId") String keycloakId) {
        return Conta.find("keycloakId", keycloakId).firstResult();
    }

    @GET
    @Path("/saldo/{keycloakId}")
    public Response buscarSaldo(@PathParam("keycloakId") String keycloakId) {
        return Conta.find("keycloakId", keycloakId).firstResultOptional()
                .map(conta -> Response.ok(Map.of("saldo", ((Conta) conta).saldo)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

}
