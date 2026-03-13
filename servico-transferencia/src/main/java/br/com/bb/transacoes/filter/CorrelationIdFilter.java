package br.com.bb.transacoes.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;
import java.util.UUID;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // 1. Tenta pegar do header, se não existir, gera um novo
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 2. "Cola o post-it" no MDC para os logs automáticos
        MDC.put(MDC_KEY, correlationId);

        // 3. Garante que o ID possa ser recuperado depois pelo Service
        requestContext.setProperty(MDC_KEY, correlationId);
    }
}