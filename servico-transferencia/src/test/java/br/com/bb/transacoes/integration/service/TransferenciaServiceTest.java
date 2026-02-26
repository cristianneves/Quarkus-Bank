package br.com.bb.transacoes.integration.service;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.TransferenciaService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@QuarkusTest
public class TransferenciaServiceTest extends BaseMessagingTest {

    @Inject
    TransferenciaService service;

    @BeforeEach
    void setupGlobalMocks() {
        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);
    }

    @Test
    @DisplayName("Deve transferir com sucesso e notificar Kafka")
    public void deveTransferirComSucesso() {
        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();
        mockContas(origem, destino);

        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));

        service.realizarTransferencia(dto);

        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));

        Assertions.assertEquals(1, connector.sink("transferencias-concluidas").received().size());
    }

    @Test
    @DisplayName("Deve falhar por Acesso Negado")
    public void deveFalharAcessoNegado() {
        when(jwt.getSubject()).thenReturn("HACKER-ID");

        mockContas(TestDataFactory.contaPadraoOrigem(), TestDataFactory.contaPadraoDestino());

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("50.00")))
        );

        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @DisplayName("Deve falhar quando o saldo é insuficiente")
    public void deveFalharSaldoInsuficiente() {
        Conta origem = TestDataFactory.novaConta(CONTA_ORIGEM, USER_ID, new BigDecimal("50.00"));
        mockContas(origem, TestDataFactory.contaPadraoDestino());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00")))
        );

        Assertions.assertEquals("Saldo insuficiente.", exception.getMessage());
        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharOrigemInexistente() {
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10.00")))
        );
    }

    @Test
    @DisplayName("Deve falhar quando a conta de destino não existe")
    public void deveFalharDestinoInexistente() {
        // GIVEN
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(TestDataFactory.contaPadraoOrigem());
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(null);

        // WHEN / THEN
        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10")))
        );
    }

    @Test
    @DisplayName("Não deve permitir transferência para a mesma conta")
    public void deveFalharMesmaConta() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_ORIGEM, new BigDecimal("10"));

        Assertions.assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
    }

    @Test
    @DisplayName("Deve ignorar transferência se a chave de idempotência já existir")
    public void deveGarantirIdempotenciaNoService() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));

        when(Transferencia.findByIdempotencyKey(dto.idempotencyKey())).thenReturn(new Transferencia());

        service.realizarTransferencia(dto);

        PanacheMock.verify(Conta.class, never()).findByNumeroWithLock(anyString());

        Assertions.assertTrue(connector.sink("transferencias-concluidas").received().isEmpty());
    }

    private void mockContas(Conta origem, Conta destino) {
        when(Conta.findByNumeroWithLock(origem.numero)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(destino.numero)).thenReturn(destino);
    }
}