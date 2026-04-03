package br.com.bb.notificacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario_notificacao")
public class Usuario extends PanacheEntityBase {

    @Id
    @Column(name = "keycloak_id", nullable = false)
    public String keycloakId;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String nome;

    public Usuario() {}

    public static Usuario findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
