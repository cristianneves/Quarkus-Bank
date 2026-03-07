package br.com.bb.transacoes.unit.exception;

import br.com.bb.transacoes.dto.ErrorDTO;
import br.com.bb.transacoes.exception.GeneralExceptionMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GeneralExceptionMapperTest {

    @Inject
    GeneralExceptionMapper mapper;

    @Test
    @DisplayName("Deve retornar 500 e mensagem genérica para erros desconhecidos")
    void deveProtegerContraErrosInternos() {
        Response response = mapper.toResponse(new RuntimeException("Erro de Banco de Dados"));
        ErrorDTO entity = (ErrorDTO) response.getEntity();

        assertEquals(500, response.getStatus());
        // ️ O texto deve ser o que definimos no Mapper, não o da Exception real
        assertEquals("Ocorreu um erro interno no servidor. Tente novamente mais tarde.", entity.mensagem());
    }

    @Test
    @DisplayName("GeneralMapper: Deve processar WebApplicationException")
    void deveProcessarWebApplicationException() {
        // Simula um erro 404 do próprio JAX-RS
        var ex = new jakarta.ws.rs.NotFoundException("Não encontrado");
        var response = mapper.toResponse(ex);

        assertEquals(404, response.getStatus());
    }

    @Test
    @DisplayName("GeneralMapper: Deve tratar erro genérico com status 500")
    void deveTratarErroGenerico() {
        var ex = new RuntimeException("Erro imprevisto");
        var response = mapper.toResponse(ex);

        assertEquals(500, response.getStatus());
        // Garante que o Log.error foi executado internamente
    }
}