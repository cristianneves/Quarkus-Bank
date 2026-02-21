package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/contas")
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

}
