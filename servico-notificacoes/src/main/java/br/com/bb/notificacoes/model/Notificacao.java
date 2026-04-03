package br.com.bb.notificacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
public class Notificacao extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificacao_gen")
    @SequenceGenerator(name = "notificacao_gen", sequenceName = "notificacao_SEQ", allocationSize = 50)
    public Long id;

    @Column(name = "keycloak_id", nullable = false)
    public String keycloakId;

    @Column(nullable = false)
    public String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String mensagem;

    @Column(nullable = false)
    public String tipo; // BEM_VINDO | DEBITO | CREDITO | ENCERRAMENTO

    @Column(nullable = false)
    public String status; // PENDENTE | ENVIADA | LIDA

    @Column(name = "aggregate_id", nullable = false)
    public String aggregateId;

    @Column(name = "correlation_id")
    public String correlationId;

    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    public LocalDateTime enviadoEm;

    public Notificacao() {}

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDENTE";
        }
    }
}
