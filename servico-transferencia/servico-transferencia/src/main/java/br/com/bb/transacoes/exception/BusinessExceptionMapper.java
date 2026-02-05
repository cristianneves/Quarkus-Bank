package br.com.bb.transacoes.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Override
    public Response toResponse(BusinessException exception) {
        ErrorDTO error = new ErrorDTO(
                exception.getMessage(),
                422,
                LocalDateTime.now()
        );

        return Response.status(422).entity(error).build();
    }
}
