package br.com.bb.transacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent extends PanacheEntity {

    @Column(nullable = false)
    public String aggregateType; // Ex: "PESSOA"

    @Column(nullable = false)
    public String aggregateId;   // O KeycloakId ou ID do banco

    @Column(nullable = false)
    public String type;          // Ex: "PESSOA_CRIADA"

    @Column(columnDefinition = "TEXT", nullable = false)
    public String payload;       // O JSON da mensagem

    @Column(name = "correlation_id", nullable = false)
    public String correlationId;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public LocalDateTime processedAt;

    public OutboxEvent() {}

    public OutboxEvent(String aggregateType, String aggregateId, String type, String payload, String correlationId) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.correlationId = correlationId;
        this.createdAt = LocalDateTime.now();
    }
}