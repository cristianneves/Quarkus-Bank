package br.com.bb.transacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Transferencia extends PanacheEntity {
    public String numeroOrigem;
    public String numeroDestino;
    public BigDecimal valor;
    public LocalDateTime dataHora;
    public String status; // CONCLUIDA, FALHA, PROCESSANDO

    @Column(unique = true) // 🛡️ O banco de dados impedirá duplicatas
    public String idempotencyKey;

    public static Transferencia findByIdempotencyKey(String key) {
        return find("idempotencyKey", key).firstResult();
    }
}