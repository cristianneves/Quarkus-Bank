package br.com.bb.transacoes.base;

import br.com.bb.transacoes.dto.PessoaEventDTO;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.model.Conta;

import java.math.BigDecimal;
import java.util.UUID;

public class TestDataFactory implements TestConstants {

    public static Conta novaConta(String numero, String userId, BigDecimal saldo) {
        Conta c = new Conta();
        c.numero = numero;
        c.keycloakId = userId;
        c.saldo = saldo;
        return c;
    }
    public static Conta contaPadraoOrigem() { return novaConta(CONTA_ORIGEM, USER_ID, SALDO_PADRAO); }
    public static Conta contaPadraoDestino() { return novaConta(CONTA_DESTINO, "destino-id", new BigDecimal("500.00")); }

    public static TransferenciaDTO novaTransferenciaDTO(String origem, String destino, BigDecimal valor) {
        return new TransferenciaDTO(
                origem,
                destino,
                valor,
                UUID.randomUUID().toString()
        );
    }

    public static PessoaEventDTO novoEventoPessoa(String keycloakId, String cpf) {
        return new PessoaEventDTO(keycloakId, "Usuario Teste", cpf, "teste@bb.com.br");
    }
}