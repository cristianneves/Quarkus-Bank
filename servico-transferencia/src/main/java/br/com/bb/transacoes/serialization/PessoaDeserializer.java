package br.com.bb.transacoes.serialization;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PessoaDeserializer extends ObjectMapperDeserializer<PessoaEventDTO> {
    public PessoaDeserializer() {
        super(PessoaEventDTO.class);
    }
}