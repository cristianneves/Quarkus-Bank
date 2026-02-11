package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @POST
    @Path("/transferir")
    @RolesAllowed("user")
    public Response transferir(@Valid TransferenciaDTO dto){
        service.realizarTransferencia(dto);
        return Response.status(Response.Status.OK).build();
    }

}
