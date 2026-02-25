package br.com.bb.transacoes.unit.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import br.com.bb.transacoes.exception.ValidationExceptionMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.Mockito.when;

class ValidationExceptionMapperTest {

    private final ValidationExceptionMapper mapper = new ValidationExceptionMapper();

    @Test
    @DisplayName("Deve converter falhas de validação em 400 com mensagens unificadas")
    void deveConverterValidationException() {
        // Mockando as violações do Hibernate Validator
        ConstraintViolation<?> v1 = Mockito.mock(ConstraintViolation.class);
        when(v1.getMessage()).thenReturn("Valor deve ser positivo");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1));

        Response response = mapper.toResponse(ex);
        ErrorDTO entity = (ErrorDTO) response.getEntity();

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(entity.mensagem().contains("Valor deve ser positivo"));
    }
}