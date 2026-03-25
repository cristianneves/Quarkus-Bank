package br.com.bb.cadastro.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception == null) {
            return Response.status(500)
                    .entity(Map.of("erro", "Erro interno no servidor", "msg", "Erro desconhecido"))
                    .build();
        }

        // 1. Erros que nós lançamos manualmente (400, 404, 409...)
        if (exception instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) exception;
            return Response.fromResponse(e.getResponse())
                    .entity(Map.of("status", e.getResponse().getStatus(), "erro", e.getMessage()))
                    .build();
        }

        // 2. Erros de validação do Hibernate (@CPF, @Email, @NotBlank)
        if (exception instanceof jakarta.validation.ConstraintViolationException) {
            var violations = (jakarta.validation.ConstraintViolationException) exception;
            String mensagens = violations.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("erro", "Validação falhou", "detalhes", mensagens))
                    .build();
        }

        // 3. Erro de Banco de Dados (Unique Constraint - CPF duplicado no flush)
        if (findCause(exception, ConstraintViolationException.class)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("erro", "Conflito de dados", "detalhe", "CPF ou E-mail já cadastrado."))
                    .build();
        }

        // 4. Erro 500 Genérico (Último caso)
        String message = exception.getMessage() != null ? exception.getMessage() : "Erro desconhecido";
        return Response.status(500)
                .entity(Map.of("erro", "Erro interno no servidor", "msg", message))
                .build();
    }

    // Função para escavar a causa real do erro
    private boolean findCause(Throwable t, Class<? extends Throwable> type) {
        while (t != null) {
            if (type.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }
}