package br.com.bb.cadastro.service;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.OutboxEvent;
import br.com.bb.cadastro.model.Pessoa;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.UUID;

@ApplicationScoped
public class PessoaService {

    @Inject
    Keycloak keycloak;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public Pessoa registrarNovoUsuario(PessoaDTO dto) {
        validarCpfUnico(dto.cpf);
        String keycloakId = criarUsuarioNoKeycloak(dto);

        try {
            atribuirRoleUsuario(keycloakId);

            Pessoa pessoa = new Pessoa();
            pessoa.nome = dto.nome;
            pessoa.cpf = dto.cpf;
            pessoa.email = dto.email;
            pessoa.keycloakId = keycloakId;
            pessoa.persist();

            // SINALIZA CRIAÇÃO
            salvarNoOutbox(pessoa, "PESSOA_CRIADA");

            return pessoa;
        } catch (Exception e) {
            Log.errorf("🚨 Falha local. Removendo usuário %s do Keycloak.", keycloakId);
            removerUsuarioNoKeycloak(keycloakId);
            throw new WebApplicationException("Erro ao salvar no banco. Cadastro revertido.", 500);
        }
    }

    @Transactional
    public Pessoa cadastrarUsuarioLogado(Pessoa pessoa, String keycloakId) {
        if (Pessoa.find("keycloakId", keycloakId).firstResult() != null) {
            throw new WebApplicationException("Usuário já cadastrado.", 409);
        }

        pessoa.keycloakId = keycloakId;
        pessoa.persist();

        // 🛠️ CORREÇÃO: Passando o segundo argumento necessário
        salvarNoOutbox(pessoa, "PESSOA_CRIADA");

        return pessoa;
    }

    @Transactional
    public void excluirUsuarioCompleto(String email) {
        Pessoa pessoa = Pessoa.find("email", email).firstResult();
        if (pessoa == null) return;

        String keycloakId = pessoa.keycloakId;

        // SINALIZA EXCLUSÃO PARA O KAFKA
        salvarNoOutbox(pessoa, "PESSOA_EXCLUIDA");

        // Limpa Keycloak e Banco Local
        removerUsuarioNoKeycloak(keycloakId);
        pessoa.delete();
        Log.infof("✅ Usuário %s removido com sucesso.", email);
    }

    private void salvarNoOutbox(Pessoa pessoa, String tipoEvento) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(pessoa);
            String cid = org.slf4j.MDC.get("correlationId");
            if (cid == null) cid = "cad-" + UUID.randomUUID();

            OutboxEvent event = new OutboxEvent(
                    "PESSOA",
                    pessoa.keycloakId,
                    tipoEvento, // Agora o campo 'type' recebe o valor correto
                    jsonPayload,
                    cid
            );
            event.persist();
        } catch (Exception e) {
            Log.error("❌ Erro ao gerar Outbox", e);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private String criarUsuarioNoKeycloak(PessoaDTO dto) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(dto.email);
        user.setEmail(dto.email);
        user.setFirstName(dto.nome);
        user.setEnabled(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(dto.password);
        cred.setTemporary(false);
        user.setCredentials(Collections.singletonList(cred));

        Response response = keycloak.realm("bank-realm").users().create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Falha na integração com Keycloak");
        }
        return CreatedResponseUtil.getCreatedId(response);
    }

    private void atribuirRoleUsuario(String userId) {
        RoleRepresentation role = keycloak.realm("bank-realm").roles().get("user").toRepresentation();
        keycloak.realm("bank-realm").users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }

    private void removerUsuarioNoKeycloak(String userId) {
        try {
            keycloak.realm("bank-realm").users().get(userId).remove();
        } catch (Exception e) {
            Log.error("Erro ao remover no Keycloak", e);
        }
    }

    private void validarCpfUnico(String cpf) {
        if (Pessoa.find("cpf", cpf).firstResult() != null) {
            throw new WebApplicationException("CPF já cadastrado!", 400);
        }
    }
}