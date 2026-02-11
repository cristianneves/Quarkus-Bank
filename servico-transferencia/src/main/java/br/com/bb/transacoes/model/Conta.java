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

    public static Conta findByNumero(String numero) {
        return find("numero", numero).firstResult();
    }
}