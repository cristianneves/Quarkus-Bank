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
import java.math.BigDecimal;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
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
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 2
    )
    @Fallback(fallbackMethod = "meuPerfilFallback")
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
        perfil.email = pessoa.email;
        perfil.keycloakId = pessoa.keycloakId;

        // 2. Busca dados da conta no microserviço de Transações
        String token = "Bearer " + jwt.getRawToken();
        ContaDTO conta = contaClient.buscarPorKeycloakId(sub, token);

        perfil.numeroConta = conta.numero;
        perfil.agencia = conta.agencia;
        perfil.saldo = conta.saldo;

        return perfil;
    }

    public PerfilDTO meuPerfilFallback() {
        Log.warn("⚠️ Circuit Breaker Ativado: Usando Fallback para Perfil");
        String sub = jwt.getSubject();
        Pessoa pessoa = Pessoa.find("keycloakId", sub).firstResult();

        if (pessoa == null) {
            throw new WebApplicationException("Perfil não localizado para o ID: " + sub, 404);
        }

        PerfilDTO perfil = new PerfilDTO();
        perfil.nome = pessoa.nome;
        perfil.cpf = pessoa.cpf;
        perfil.email = pessoa.email;
        perfil.keycloakId = pessoa.keycloakId;

        // Dados de Fallback (Saldo indisponível)
        perfil.numeroConta = "N/A";
        perfil.agencia = "N/A";
        perfil.saldo = BigDecimal.ZERO; 
        
        // Adicionamos um log indicando que o saldo é parcial
        Log.info("ℹ️ Dados bancários indisponíveis. Retornando apenas dados cadastrais.");
        
        return perfil;
    }
}
