package br.com.bb.transacoes.unit.base;

import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.base.TestConstants;
import java.math.BigDecimal;

public class TestDataFactory implements TestConstants {

    public static Conta novaConta(String numero, String userId, BigDecimal saldo) {
        Conta c = new Conta();
        c.numero = numero;
        c.keycloakId = userId;
        c.saldo = saldo;
        return c;
    }

    public static Conta contaPadraoOrigem() {
        return novaConta(CONTA_ORIGEM, USER_ID, SALDO_PADRAO);
    }

    public static Conta contaPadraoDestino() {
        return novaConta(CONTA_DESTINO, "destino-id", new BigDecimal("500.00"));
    }
}