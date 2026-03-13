package br.com.bb.transacoes.integration.messaging;

import br.com.bb.transacoes.filter.CorrelationIdFilter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CorrelationIdFilterTest {

    @Inject
    CorrelationIdFilter filter;

    @Test
    @DisplayName("Filtro: Deve gerar novo Correlation ID se o header estiver vazio")
    void deveGerarNovoIdSeVazio() {
        ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getHeaderString(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

        filter.filter(context);

        // Verifica se o ID foi "colado" no contexto da requisição
        verify(context).setProperty(eq(CorrelationIdFilter.MDC_KEY), anyString());
        assertNotNull(org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
