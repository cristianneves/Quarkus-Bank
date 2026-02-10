package br.com.bb.cadastro.rest;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.Pessoa;
import br.com.bb.cadastro.service.PessoaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
    @Transactional
    @RolesAllowed("user")
    public Response cadastrar(Pessoa pessoa) {
        // Extraímos o ID único do Keycloak (campo 'sub')
        String sub = jwt.getSubject();

        pessoa.keycloakId = sub;
        pessoa.persist();

        return Response.status(Response.Status.CREATED).entity(pessoa).build();
    }

    @POST
    @Path("/registrar") // Novo endpoint público
    @PermitAll // Qualquer um pode acessar para se cadastrar
    public Response registrar(PessoaDTO dto) {
        Pessoa pessoa = pessoaService.registrarNovoUsuario(dto);
        return Response.status(Response.Status.CREATED).entity(pessoa).build();
    }
}
