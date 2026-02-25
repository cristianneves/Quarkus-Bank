package br.com.bb.cadastro.rest;

import org.eclipse.microprofile.jwt.JsonWebToken;
import br.com.bb.cadastro.client.ContaClient;
import br.com.bb.cadastro.dto.ContaDTO;
import br.com.bb.cadastro.dto.PerfilDTO;
import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/perfil")
@Authenticated
public class PerfilResource {

    @Inject
    JsonWebToken jwt; // Injeta o token vindo do Keycloak

    @Inject
    @RestClient
    ContaClient contaClient;

    @GET
    public PerfilDTO meuPerfil() {
        // 1. Extrair o ID Único do Keycloak
        String keycloakId = jwt.getSubject();

        // 2. Buscar dados pessoais no banco 5432
        Pessoa pessoa = Pessoa.find("keycloakId", keycloakId).firstResult();

        // 3. Preparar o Token para envio (Formato: Bearer <token>)
        String tokenFormatado = "Bearer " + jwt.getRawToken();

        // 4. Buscar dados da conta no banco 5433 repassando o token
        ContaDTO conta = contaClient.buscarPorKeycloakId(keycloakId, tokenFormatado);

        // 5. Montar a resposta agregada
        PerfilDTO perfil = new PerfilDTO();
        perfil.nome = pessoa.nome;
        perfil.cpf = pessoa.cpf;
        perfil.email = pessoa.email;
        perfil.keycloakId = keycloakId;

        if (conta != null) {
            perfil.numeroConta = conta.numero;
            perfil.agencia = conta.agencia;
            perfil.saldo = conta.saldo;
        }

        return perfil;
    }
}
