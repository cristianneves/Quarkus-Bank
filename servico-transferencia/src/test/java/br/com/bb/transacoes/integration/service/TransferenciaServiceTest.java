package br.com.bb.transacoes.integration.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.integration.base.BaseIntegrationTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.TransferenciaService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@QuarkusTest
public class TransferenciaServiceTest extends BaseIntegrationTest {

    @Inject
    TransferenciaService service;

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    void setupGlobalMocks() {
        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);
    }

    private TransferenciaDTO criarDTO(BigDecimal valor) {
        return new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, valor, UUID.randomUUID().toString());
    }

    private Conta criarConta(String numero, String ownerId) {
        Conta c = new Conta();
        c.numero = numero;
        c.saldo = SALDO_PADRAO;
        c.keycloakId = ownerId;
        return c;
    }

    private void mockContas(Conta origem, Conta destino) {
        when(Conta.findByNumeroWithLock(origem.numero)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(destino.numero)).thenReturn(destino);
    }


    @Test
    @DisplayName("Deve transferir com sucesso")
    public void deveTransferirComSucesso() {
        Conta origem = criarConta(CONTA_ORIGEM, USER_ID);
        Conta destino = criarConta(CONTA_DESTINO, "outro-id");
        mockContas(origem, destino);

        service.realizarTransferencia(criarDTO(new BigDecimal("100.00")));

        Assertions.assertEquals(new BigDecimal("900.00"), origem.saldo);
    }

    @Test
    @DisplayName("Deve falhar por Acesso Negado")
    public void deveFalharAcessoNegado() {
        when(jwt.getSubject()).thenReturn("HACKER-ID");

        Conta origem = criarConta(CONTA_ORIGEM, USER_ID);
        mockContas(origem, criarConta(CONTA_DESTINO, "outro-id"));

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(criarDTO(new BigDecimal("50.00")))
        );

        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @DisplayName("Deve falhar quando o saldo é insuficiente")
    public void deveFalharSaldoInsuficiente() {
        Conta origem = criarConta(CONTA_ORIGEM, USER_ID);
        origem.saldo = new BigDecimal("50.00");
        mockContas(origem, criarConta(CONTA_DESTINO, "outro-id"));

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(criarDTO(new BigDecimal("100.00")))
        );

        Assertions.assertEquals("Saldo insuficiente.", exception.getMessage());
        Assertions.assertEquals(new BigDecimal("50.00"), origem.saldo);
        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharOrigemInexistente() {
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(criarDTO(new BigDecimal("10.00")))
        );
    }

    @Test
    @DisplayName("Deve falhar quando a conta de destino não existe")
    public void deveFalharDestinoInexistente() {
        Conta origem = criarConta(CONTA_ORIGEM, USER_ID);
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(criarDTO(new BigDecimal("10.00")))
        );
    }

    @Test
    @DisplayName("Não deve permitir transferência para a mesma conta")
    public void deveFalharMesmaConta() {
        TransferenciaDTO dtoMesmaConta = new TransferenciaDTO(
                CONTA_ORIGEM, CONTA_ORIGEM, new BigDecimal("10.00"), UUID.randomUUID().toString()
        );

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(dtoMesmaConta)
        );
    }
}