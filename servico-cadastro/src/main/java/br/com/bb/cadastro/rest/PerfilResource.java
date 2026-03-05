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
    @Authenticated // Garante que só usuários logados entrem
    public PerfilDTO meuPerfil() {
        String sub = jwt.getSubject();

        // 1. Busca a pessoa pelo ID do Keycloak
        Pessoa pessoa = Pessoa.find("keycloakId", sub).firstResult();

        // 🚀 O SEGREDO DO 404: Se não achar, lança a exceção correta do JAX-RS
        if (pessoa == null) {
            throw new WebApplicationException("Perfil não localizado para o ID: " + sub, 404);
        }

        // 2. Se chegou aqui, a pessoa existe. Agora é seguro chamar getNome()
        PerfilDTO perfil = new PerfilDTO();
        perfil.nome = pessoa.nome; // Sem risco de NullPointerException agora
        perfil.cpf = pessoa.cpf;

        // 3. Busca dados da conta no outro microserviço
        try {
            String token = "Bearer " + jwt.getRawToken();
            ContaDTO conta = contaClient.buscarPorKeycloakId(sub, token);

            perfil.numeroConta = conta.numero;
            perfil.agencia = conta.agencia;
            perfil.saldo = conta.saldo;
        } catch (Exception e) {
            Log.error("Falha ao integrar com serviço de contas", e);
            throw new WebApplicationException("Erro ao buscar dados da conta no serviço de transações", 502);
        }

        return perfil;
    }
}
