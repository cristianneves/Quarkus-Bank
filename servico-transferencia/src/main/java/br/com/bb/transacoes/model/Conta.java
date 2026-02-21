package br.com.bb.transacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;

@Entity
public class Conta extends PanacheEntity {
    public String numero;
    public String agencia;
    public BigDecimal saldo;

    @Column(unique = true, nullable = false)
    public String keycloakId;

    public Conta() {} // Obrigatório para o Hibernate

    public Conta(String numero, BigDecimal saldo) {
        this.numero = numero;
        this.saldo = saldo;
    }

    public static Conta findByNumero(String numero) {
        return find("numero", numero).firstResult();
    }

    // Em vez de findByNumero comum, usamos o LockMode
    public static Conta findByNumeroWithLock(String numero) {
        // O LockModeType.PESSIMISTIC_WRITE trava a linha no banco até o @Transactional terminar
        return find("numero", numero)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
    }
}