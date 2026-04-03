package br.com.bb.cadastro.service;

import br.com.bb.cadastro.client.ContaClient;
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
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
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

    @Inject
    @RestClient
    ContaClient contaClient;

    @Inject
    JsonWebToken jwt;

    @Transactional
    public Pessoa registrarNovoUsuario(PessoaDTO dto) {
        // 1. Validações prévias lançando 409 (Conflict)
        validarCpfUnico(dto.cpf);
        validarEmailUnico(dto.email);

        String keycloakId = criarUsuarioNoKeycloak(dto);

        try {
            atribuirRoleUsuario(keycloakId);

            Pessoa pessoa = new Pessoa();
            pessoa.nome = dto.nome;
            pessoa.cpf = dto.cpf;
            pessoa.email = dto.email;
            pessoa.keycloakId = keycloakId;

            pessoa.persistAndFlush();

            salvarNoOutbox(pessoa, "PESSOA_CRIADA");
            return pessoa;

        } catch (Exception e) {
            // 2. COMPENSAÇÃO: Se der qualquer erro no banco, limpa o Keycloak
            Log.errorf("🚨 Erro na persistência. Limpando Keycloak ID: %s", keycloakId);
            removerUsuarioNoKeycloak(keycloakId);

            // 3. RELANÇA O ERRO: Não force 500 aqui
            throw e;
        }
    }

    @Transactional
    public Pessoa cadastrarUsuarioLogado(Pessoa pessoa, String keycloakId) {
        if (Pessoa.find("keycloakId", keycloakId).firstResult() != null) {
            throw new WebApplicationException("Usuário já cadastrado.", 409);
        }

        pessoa.keycloakId = keycloakId;
        pessoa.persist();

        salvarNoOutbox(pessoa, "PESSOA_CRIADA");

        return pessoa;
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
    public void excluirUsuarioCompleto(String email) {
        // 1. Localiza a pessoa para obter os dados
        Pessoa pessoa = Pessoa.find("email", email).firstResult();
        if (pessoa == null) {
            Log.info("ℹ️ Pessoa não encontrada no banco. Abortando.");
            return;
        }

        try {
            String token = "Bearer " + jwt.getRawToken();
            var resposta = contaClient.obterSaldo(pessoa.keycloakId, token);
            double saldo = Double.parseDouble(resposta.get("saldo").toString());

            if (saldo > 0) {
                throw new WebApplicationException(
                        "Não é possível excluir conta com saldo positivo (R$ " + saldo + "). Zere a conta primeiro!",
                        Response.Status.BAD_REQUEST
                );
            }
        } catch (Exception e) {
            if (e instanceof WebApplicationException) throw e;
            
            // Se a conta não existir (404), podemos prosseguir com a exclusão da pessoa
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                Log.infof("ℹ️ Conta não encontrada para %s. Prosseguindo com exclusão.", pessoa.keycloakId);
            } else {
                Log.error("⚠️ Não foi possível verificar o saldo. Exclusão abortada por segurança.", e);
                throw new WebApplicationException(
                        "Não foi possível validar o saldo da conta no momento. Tente novamente mais tarde.",
                        Response.Status.SERVICE_UNAVAILABLE
                );
            }
        }

        String keycloakId = pessoa.keycloakId;

        // 2. SINALIZA EXCLUSÃO PARA O KAFKA (Via Outbox)
        // Precisamos fazer isso antes de deletar a pessoa do banco local
        salvarNoOutbox(pessoa, "PESSOA_EXCLUIDA");

        // 3. Limpa Keycloak
        removerUsuarioNoKeycloak(keycloakId);

        // 4. Limpa Banco Local (Tabela Pessoa)
        pessoa.delete();

        Log.infof("✅ Usuário %s removido com sucesso de todos os sistemas.", email);
    }



    // --- MÉTODOS AUXILIARES ---

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
            Log.error("❌ Erro ao gerar Outbox. Operação será revertida para manter consistência.", e);
            throw new IllegalStateException("Falha ao registrar evento no Outbox.", e);
        }
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
            throw new WebApplicationException("O CPF " + cpf + " já está cadastrado.", Response.Status.CONFLICT);
        }
    }

    private void validarEmailUnico(String email) {
        if (Pessoa.find("email", email).firstResult() != null) {
            throw new WebApplicationException("O e-mail " + email + " já está em uso.", Response.Status.CONFLICT);
        }
    }
}