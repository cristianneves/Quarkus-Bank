package br.com.bb.cadastro.client;

import br.com.bb.cadastro.dto.ContaDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "conta-api")
public interface ContaClient {

    @GET
    @Path("/detalhes/{keycloakId}")
    ContaDTO buscarPorKeycloakId(
            @PathParam("keycloakId") String keycloakId,
            @HeaderParam("Authorization") String authHeader
    );
}