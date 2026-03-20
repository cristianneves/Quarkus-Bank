package br.com.bb.cadastro.rest;

import br.com.bb.cadastro.client.ContaClient;
import br.com.bb.cadastro.dto.ContaDTO;
import br.com.bb.cadastro.dto.PerfilDTO;
import br.com.bb.cadastro.model.Pessoa;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.jwt.JsonWebToken;
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
    @Authenticated
    public PerfilDTO meuPerfil() {
        String sub = jwt.getSubject();

        // 1. Busca a pessoa pelo ID do Keycloak (sub)
        Pessoa pessoa = Pessoa.find("keycloakId", sub).firstResult();

        if (pessoa == null) {
            throw new WebApplicationException("Perfil não localizado para o ID: " + sub, 404);
        }

        PerfilDTO perfil = new PerfilDTO();
        // --- MAPEAMENTO DOS DADOS LOCAIS ---
        perfil.nome = pessoa.nome;
        perfil.cpf = pessoa.cpf;
        perfil.email = pessoa.email;       // 👈 Faltava essa linha!
        perfil.keycloakId = pessoa.keycloakId; // 👈 E essa também!

        // 2. Busca dados da conta no microserviço de Transações
        try {
            String token = "Bearer " + jwt.getRawToken();
            ContaDTO conta = contaClient.buscarPorKeycloakId(sub, token);

            perfil.numeroConta = conta.numero;
            perfil.agencia = conta.agencia;
            perfil.saldo = conta.saldo;
        } catch (Exception e) {
            Log.error("Falha ao integrar com serviço de contas", e);
            throw new WebApplicationException("Erro ao buscar dados bancários", 502);
        }

        return perfil;
    }
}
