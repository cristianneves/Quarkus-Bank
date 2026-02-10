package br.com.bb.cadastro.service;

import br.com.bb.cadastro.dto.PessoaDTO;
import br.com.bb.cadastro.model.Pessoa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;

@ApplicationScoped
public class PessoaService {

    @Inject
    Keycloak keycloak; // O cliente que instalamos

    @Inject
    @Channel("pessoa-criada") // Nome do canal que configuraremos no properties
    Emitter<Pessoa> pessoaEmitter;

    @Transactional
    public Pessoa registrarNovoUsuario(PessoaDTO dto) {
        if (Pessoa.find("cpf", dto.cpf).firstResult() != null) {
            throw new WebApplicationException("CPF já cadastrado no banco!", 400);
        }
        // 1. Criar o usuário no Keycloak
        UserRepresentation user = new UserRepresentation();
        user.setUsername(dto.email);
        user.setEmail(dto.email);
        user.setFirstName(dto.nome);
        user.setEmailVerified(true);
        user.setEnabled(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(dto.password);
        cred.setTemporary(false);
        user.setCredentials(Collections.singletonList(cred));

        Response response = keycloak.realm("bank-realm").users().create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Erro ao criar no Keycloak: " + response.getStatus());
        }

        String keycloakId = CreatedResponseUtil.getCreatedId(response);

        // 🚀 2. ATRIBUIR ROLE 'user' (O detalhe que faltava)
        RoleRepresentation userRole = keycloak.realm("bank-realm").roles().get("user").toRepresentation();
        keycloak.realm("bank-realm").users().get(keycloakId).roles().realmLevel().add(Collections.singletonList(userRole));

        // 3. Salvar no Postgres 17
        Pessoa pessoa = new Pessoa();
        pessoa.nome = dto.nome;
        pessoa.cpf = dto.cpf;
        pessoa.email = dto.email;
        pessoa.keycloakId = keycloakId;
        pessoa.persist();

        pessoaEmitter.send(pessoa);

        return pessoa;
    }
}