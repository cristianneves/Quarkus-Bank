package br.com.bb.transacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
public class Conta extends PanacheEntity {
    public String numero;
    public String agencia;
    public BigDecimal saldo;
    public String keycloakId;

    public Conta() {} // Obrigatório para o Hibernate

    public Conta(String numero, BigDecimal saldo) {
        this.numero = numero;
        this.saldo = saldo;
    }

    public static Conta findByNumero(String numero) {
        return find("numero", numero).firstResult();
    }
}