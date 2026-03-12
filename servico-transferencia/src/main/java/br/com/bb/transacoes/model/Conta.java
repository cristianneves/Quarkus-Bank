package br.com.bb.transacoes.model;

import br.com.bb.transacoes.exception.BusinessException;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;

@Entity
public class Conta extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String numero;

    @Column(nullable = false)
    public String agencia;

    @Column(nullable = false)
    public BigDecimal saldo;

    @Column(unique = true, nullable = false)
    public String keycloakId;

    public String nomeTitular;

    @Column(nullable = false) // ✅ CPF não pode ser nulo!
    public String cpfTitular;

    public String emailTitular;


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

    public void debitar(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor de débito deve ser positivo.");
        }
        if (this.saldo.compareTo(valor) < 0) {
            throw new BusinessException("Saldo insuficiente para a operação.");
        }
        this.saldo = this.saldo.subtract(valor);
    }

    public void creditar(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor de crédito deve ser positivo.");
        }
        this.saldo = this.saldo.add(valor);
    }
}