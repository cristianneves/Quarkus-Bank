package br.com.bb.cadastro.rest;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    @POST
    @RolesAllowed("user")
    public Response cadastrar(Pessoa pessoa) {
        String sub = jwt.getSubject();

        // 🚀 O Service agora garante a Pessoa + Outbox em uma só transação
        Pessoa cadastrada = pessoaService.cadastrarUsuarioLogado(pessoa, sub);

        return Response.status(Response.Status.CREATED).entity(cadastrada).build();
    }

    @POST
    @Path("/registrar") // Novo endpoint público
    @PermitAll // Qualquer um pode acessar para se cadastrar
    public Response registrar(PessoaDTO dto) {
        Pessoa pessoa = pessoaService.registrarNovoUsuario(dto);
        return Response.status(Response.Status.CREATED).entity(pessoa).build();
    }

    // Adicione este método ao final da sua classe PessoaResource
    @DELETE
    @Path("/{email}")
    @PermitAll // Aberto para facilitar seus testes agora
    public Response excluir(@PathParam("email") String email) {
        pessoaService.excluirUsuarioCompleto(email);
        return Response.noContent().build();
    }
}
