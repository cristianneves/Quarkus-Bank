package br.com.bb.cadastro.rest;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/pessoas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PessoaResource {

    @Inject
    JsonWebToken jwt;
    @Inject
    PessoaService pessoaService;
    @Inject
    SecurityIdentity identity;

    @POST
    @RolesAllowed("user")
    public Response cadastrar(Pessoa pessoa) {
        String sub = jwt.getSubject();

        // 🚀 O Service agora garante a Pessoa + Outbox em uma só transação
        Pessoa cadastrada = pessoaService.cadastrarUsuarioLogado(pessoa, sub);

        return Response.status(Response.Status.CREATED).entity(cadastrada).build();
    }

    @POST
    @Path("/registrar")
    @PermitAll
    public Response registrar(@Valid PessoaDTO dto) {
        Pessoa pessoa = pessoaService.registrarNovoUsuario(dto);
        return Response.status(Response.Status.CREATED).entity(pessoa).build();
    }

    @DELETE
    @Path("/{email}")
    @RolesAllowed({"user", "admin"})
    public Response excluir(@PathParam("email") String email) {
        Pessoa pessoa = Pessoa.find("email", email).firstResult();
        if (pessoa != null && !identity.hasRole("admin")) {
            String sub = jwt.getSubject();
            if (sub == null || !sub.equals(pessoa.keycloakId)) {
                throw new ForbiddenException("Acesso negado para excluir este usuário.");
            }
        }

        pessoaService.excluirUsuarioCompleto(email);
        return Response.noContent().build();
    }
}
