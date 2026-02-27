package br.com.bb.transacoes.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webAppException) {
            return webAppException.getResponse();
        }

        Log.error("Erro capturado pelo Mapper: ", exception);
        ErrorDTO error = new ErrorDTO(
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
                500,
                LocalDateTime.now()
        );

        return Response.status(500).entity(error).build();
    }
}
