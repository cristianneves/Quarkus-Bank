package br.com.bb.transacoes.rest;

import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.interceptor.AuditAdmin;
import br.com.bb.transacoes.model.Auditoria;
import br.com.bb.transacoes.service.AdminService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;

@Path("/api/admin")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    AdminService adminService;

    @POST
    @Path("/deposito")
    @AuditAdmin
    public Response depositar(DepositoDTO dto) {
        adminService.realizarDeposito(dto);
        return Response.ok().build();
    }

    @GET
    @Path("/auditoria")
    public List<Auditoria> listarLogs(@QueryParam("data") LocalDate data) {
        LocalDate dataBusca = (data != null) ? data : LocalDate.now();
        return adminService.consultarLogsPorData(dataBusca);
    }
}