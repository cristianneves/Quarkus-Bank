package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.service.TransferenciaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/transferencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransferenciaResource {

    @Inject
    TransferenciaService service;

    @POST
    @RolesAllowed("user")
    public Response realizar(@Valid TransferenciaDTO dto) {
        service.realizarTransferencia(dto);
        // Retornamos 201 Created porque agora geramos um registro de Transferencia no banco
        return Response.status(Response.Status.CREATED).build();
    }
}