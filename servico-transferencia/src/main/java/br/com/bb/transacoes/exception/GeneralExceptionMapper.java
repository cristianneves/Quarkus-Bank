package br.com.bb.transacoes.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        ErrorDTO error = new ErrorDTO(
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
                500,
                LocalDateTime.now()
        );

        return Response.status(500).entity(error).build();
    }
}
