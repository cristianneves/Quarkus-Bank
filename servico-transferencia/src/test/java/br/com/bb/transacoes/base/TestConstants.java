package br.com.bb.transacoes.base;

import java.math.BigDecimal;

public interface TestConstants {
    String USER_ID = "user-origem-id";
    String CONTA_ORIGEM = "12345-6";
    String CONTA_DESTINO = "54321-0";
    BigDecimal SALDO_PADRAO = new BigDecimal("1000.00");
}