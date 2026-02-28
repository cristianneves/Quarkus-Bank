package br.com.bb.transacoes.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "audit_log")
public class Auditoria extends PanacheEntity {

    public String usuario;    // Quem fez (Admin Name/Email)
    public String acao;       // O que fez (Nome do método)
    public String detalhes;   // Parâmetros (JSON ou String)
    public LocalDateTime dataHora;

    public Auditoria() {}

    public Auditoria(String usuario, String acao, String detalhes) {
        this.usuario = usuario;
        this.acao = acao;
        this.detalhes = detalhes;
        this.dataHora = LocalDateTime.now();
    }

    public static List<Auditoria> buscarPorData(LocalDate data) {
        // Definimos o início do dia (00:00:00) e o fim do dia (23:59:59)
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);

        return find("dataHora >= ?1 and dataHora <= ?2 order by dataHora desc",
                inicioDia, fimDia).list();
    }
}