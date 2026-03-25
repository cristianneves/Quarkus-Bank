package br.com.bb.cadastro.rest;

import br.com.bb.cadastro.dto.PessoaDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import java.util.Set;

@Path("/test-exception")
public class TestExceptionResource {

    @GET
    @Path("/web-exception-404")
    public Response webException404() {
        throw new WebApplicationException("Recurso não encontrado", 404);
    }

    @GET
    @Path("/web-exception-409")
    public Response webException409() {
        throw new WebApplicationException("Conflito", 409);
    }

    @GET
    @Path("/web-exception-500")
    public Response webException500() {
        throw new WebApplicationException("Erro interno", 500);
    }

    @GET
    @Path("/generic-exception")
    public Response genericException() {
        throw new RuntimeException("Erro genérico do servidor");
    }

    @POST
    @Path("/validation-test")
    public Response validationTest(@Valid PessoaDTO dto) {
        return Response.ok().build();
    }

    @GET
    @Path("/force-validation-exception")
    public Response forceValidationException() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        PessoaDTO dto = new PessoaDTO();
        dto.nome = "";
        dto.cpf = "123";
        dto.email = "invalid";
        dto.password = "";
        
        Set<ConstraintViolation<PessoaDTO>> violations = validator.validate(dto);
        throw new ConstraintViolationException(violations);
    }

    @GET
    @Path("/force-db-exception")
    public Response forceDBException() {
        throw new RuntimeException(new org.hibernate.exception.ConstraintViolationException("Duplicate key", null, "uq_cpf"));
    }
}