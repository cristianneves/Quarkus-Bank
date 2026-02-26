package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

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

    @POST
    @Path("/deposito")
    @RolesAllowed("admin")
    public Response depositar(DepositoDTO dto) {
        service.depositar(dto);
        return Response.ok().build();
    }

}
