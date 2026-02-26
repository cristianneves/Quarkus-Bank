package br.com.bb.transacoes.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String mensagens = exception.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining(", "));

        ErrorDTO error = new ErrorDTO(mensagens, 400, LocalDateTime.now());
        return Response.status(400).entity(error).build();
    }
}