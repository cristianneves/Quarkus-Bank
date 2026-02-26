package br.com.bb.transacoes.unit.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import br.com.bb.transacoes.exception.GeneralExceptionMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneralExceptionMapperTest {

    private final GeneralExceptionMapper mapper = new GeneralExceptionMapper();

    @Test
    @DisplayName("Deve retornar 500 e mensagem genérica para erros desconhecidos")
    void deveProtegerContraErrosInternos() {
        Response response = mapper.toResponse(new RuntimeException("Erro de Banco de Dados"));
        ErrorDTO entity = (ErrorDTO) response.getEntity();

        Assertions.assertEquals(500, response.getStatus());
        // ️ O texto deve ser o que definimos no Mapper, não o da Exception real
        Assertions.assertEquals("Ocorreu um erro interno no servidor. Tente novamente mais tarde.", entity.mensagem());
    }
}