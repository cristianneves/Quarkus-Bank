package br.com.bb.transacoes.base;

import java.math.BigDecimal;

public interface TestConstants {
    String USER_ID = "user-origem-id";
    String CONTA_ORIGEM = "123456";
    String CONTA_DESTINO = "543210";
    BigDecimal SALDO_PADRAO = new BigDecimal("1000.00");
}