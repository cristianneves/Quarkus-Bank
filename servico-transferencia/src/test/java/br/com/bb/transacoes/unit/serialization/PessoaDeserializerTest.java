package br.com.bb.transacoes.unit.serialization;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.serialization.PessoaDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class PessoaDeserializerTest {

    private final PessoaDeserializer deserializer = new PessoaDeserializer();

    @Test
    @DisplayName("Deve converter JSON válido em PessoaEventDTO")
    void deveDeserializarJsonValido() {
        // GIVEN: Um JSON que simula o que o Kafka enviaria
        String json = """
                {
                    "keycloakId": "user-123",
                    "nome": "João Silva",
                    "cpf": "12345678901",
                    "email": "joao@bb.com.br"
                }
                """;
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // WHEN
        PessoaEventDTO dto = deserializer.deserialize("pessoa-registrada", data);

        // THEN
        Assertions.assertNotNull(dto);
        Assertions.assertEquals("user-123", dto.keycloakId());
        Assertions.assertEquals("João Silva", dto.nome());
    }

    @Test
    @DisplayName("Deve lançar erro ao receber JSON com formato inválido")
    void deveFalharParaJsonInvalido() {
        byte[] data = "isso-nao-e-um-json".getBytes(StandardCharsets.UTF_8);

        // O ObjectMapperDeserializer do Quarkus lança RuntimeException em falhas críticas
        Assertions.assertThrows(RuntimeException.class, () ->
                deserializer.deserialize("pessoa-registrada", data)
        );
    }
}