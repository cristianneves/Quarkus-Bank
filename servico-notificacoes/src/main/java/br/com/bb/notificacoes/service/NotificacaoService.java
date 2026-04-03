package br.com.bb.notificacoes.service;

import br.com.bb.notificacoes.model.Notificacao;
import br.com.bb.notificacoes.model.Usuario;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class NotificacaoService {

    @Transactional
    public void registrarNotificacao(String keycloakId, String titulo, String mensagem, String tipo, String aggregateId, String correlationId) {
        Notificacao notificacao = new Notificacao();
        notificacao.keycloakId = keycloakId;
        notificacao.titulo = titulo;
        notificacao.mensagem = mensagem;
        notificacao.tipo = tipo;
        notificacao.aggregateId = aggregateId;
        notificacao.correlationId = correlationId;
        notificacao.status = "PENDENTE";
        notificacao.criadoEm = LocalDateTime.now();
        
        notificacao.persist();
        
        Log.infof("🔔 Notificação %s registrada para o usuário %s (Tipo: %s)", aggregateId, keycloakId, tipo);
        
        simularEnvioEmail(notificacao);
    }

    @Transactional
    public void atualizarOuCriarUsuario(String keycloakId, String email, String nome) {
        // Primeiro, verificamos se o email já está em uso por outro keycloakId
        Usuario existingByEmail = Usuario.findByEmail(email);
        if (existingByEmail != null && !existingByEmail.keycloakId.equals(keycloakId)) {
            Log.warnf("⚠️ Email %s já estava associado ao keycloakId %s. Removendo registro antigo para permitir nova associação com %s.", 
                    email, existingByEmail.keycloakId, keycloakId);
            existingByEmail.delete();
            // Precisamos garantir que o flush aconteça para evitar conflito imediato no persist()
            existingByEmail.flush();
        }

        Usuario usuario = Usuario.findById(keycloakId);
        if (usuario == null) {
            usuario = new Usuario();
            usuario.keycloakId = keycloakId;
        }
        usuario.email = email;
        usuario.nome = nome;
        usuario.persist();
        Log.infof("👤 Usuário %s (%s) sincronizado no serviço de notificações.", nome, keycloakId);
    }

    @Transactional
    public void removerUsuario(String keycloakId) {
        Usuario usuario = Usuario.findById(keycloakId);
        if (usuario != null) {
            usuario.delete();
            Log.infof("👤 Usuário %s removido do serviço de notificações.", keycloakId);
        }
    }

    private void simularEnvioEmail(Notificacao notificacao) {
        // Em um sistema real, isso enviaria um e-mail de verdade (SMTP, SendGrid, etc.)
        Log.infof("📧 SIMULAÇÃO DE ENVIO: Para: %s | Título: %s | Mensagem: %s", 
                notificacao.keycloakId, notificacao.titulo, notificacao.mensagem);
        
        notificacao.status = "ENVIADA";
        notificacao.enviadoEm = LocalDateTime.now();
        notificacao.persist();
    }
}
