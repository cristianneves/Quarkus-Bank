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

@ApplicationScoped
public class PessoaService {

    @Inject
    Keycloak keycloak;

    @Inject
    ObjectMapper objectMapper; // Para serializar o evento

    @Transactional
    public Pessoa registrarNovoUsuario(PessoaDTO dto) {
        validarCpfUnico(dto.cpf);

        // 1. Registro no Keycloak (Fluxo externo)
        String keycloakId = criarUsuarioNoKeycloak(dto);
        atribuirRoleUsuario(keycloakId);

        // 2. Persistência de Negócio
        Pessoa pessoa = new Pessoa();
        pessoa.nome = dto.nome;
        pessoa.cpf = dto.cpf;
        pessoa.email = dto.email;
        pessoa.keycloakId = keycloakId;
        pessoa.persist();

        // 3. REGISTRO NO OUTBOX (A mágica da consistência)
        salvarNoOutbox(pessoa);

        return pessoa;
    }

    private void validarCpfUnico(String cpf) {
        if (Pessoa.find("cpf", cpf).firstResult() != null) {
            throw new WebApplicationException("CPF já cadastrado!", 400);
        }
    }

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

    @Transactional
    public Pessoa cadastrarUsuarioLogado(Pessoa pessoa, String keycloakId) {
        // 1. Verificamos se ele já não existe para evitar duplicidade
        if (Pessoa.find("keycloakId", keycloakId).firstResult() != null) {
            throw new WebApplicationException("Usuário já cadastrado.", 409);
        }

        pessoa.keycloakId = keycloakId;
        pessoa.persist();

        // 2. AGORA GARANTIMOS O OUTBOX AQUI TAMBÉM!
        salvarNoOutbox(pessoa);

        return pessoa;
    }

    // Extraímos a lógica do Outbox para um método privado para evitar repetição (DRY)
    private void salvarNoOutbox(Pessoa pessoa) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(pessoa);

            String correlationId = org.slf4j.MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = "cad-" + java.util.UUID.randomUUID();
            }

            // Criamos o evento COM o Correlation ID (Certifique-se que o construtor da Entidade aceite esse campo)
            OutboxEvent event = new OutboxEvent(
                    "PESSOA",
                    pessoa.keycloakId,
                    "PESSOA_CRIADA",
                    jsonPayload,
                    correlationId
            );
            event.persist();
        } catch (Exception e) {
            Log.error("❌ Falha ao registrar outbox de pessoa", e);
            throw new RuntimeException("Erro ao gerar evento de outbox", e);
        }
    }

    private void atribuirRoleUsuario(String userId) {
        RoleRepresentation role = keycloak.realm("bank-realm").roles().get("user").toRepresentation();
        keycloak.realm("bank-realm").users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }
}