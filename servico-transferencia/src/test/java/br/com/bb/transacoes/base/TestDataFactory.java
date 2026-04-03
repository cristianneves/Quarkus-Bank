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
    public static Conta contaPadraoOrigem() {
        Conta c = new Conta();
        c.numero = "123456";
        c.keycloakId = USER_ID;
        c.agencia = "0001";
        c.saldo = new BigDecimal("1000.00");
        c.cpfTitular = "381.337.050-02";
        c.nomeTitular = "Cliente Origem";
        c.emailTitular = "origem@bb.com.br";
        return c;
    }

    public static Conta contaPadraoDestino() {
        Conta c = new Conta();
        c.numero = "543210";
        c.keycloakId = "user-destino-id";
        c.agencia = "0001";
        c.saldo = new BigDecimal("500.00");
        c.cpfTitular = "381.337.050-02";
        c.nomeTitular = "Cliente Destino";
        c.emailTitular = "destino@bb.com.br";
        return c;
    }
    public static TransferenciaDTO novaTransferenciaDTO(String origem, String destino, BigDecimal valor) {
        return new TransferenciaDTO(
                origem,
                destino,
                valor,
                UUID.randomUUID().toString(),
                null,
                null
        );
    }

    public static PessoaEventDTO novoEventoPessoa(String keycloakId, String cpf) {
        return new PessoaEventDTO(keycloakId, "Usuario Teste", cpf, "teste@bb.com.br");
    }
}