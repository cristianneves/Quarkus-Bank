package br.com.bb.transacoes.unit.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.exception.BusinessExceptionMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BusinessExceptionMapperTest {

    private final BusinessExceptionMapper mapper = new BusinessExceptionMapper();

    @Test
    @DisplayName("Deve converter BusinessException em ErrorDTO com status 422")
    void deveConverterExcecaoCorretamente() {
        String mensagemErro = "Saldo insuficiente";
        BusinessException ex = new BusinessException(mensagemErro);

        Response response = mapper.toResponse(ex);
        ErrorDTO dto = (ErrorDTO) response.getEntity();

        Assertions.assertEquals(422, response.getStatus());
        Assertions.assertEquals(mensagemErro, dto.mensagem());
        Assertions.assertNotNull(dto.timestamp());
    }
}