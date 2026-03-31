package br.com.bb.transacoes.integration.service;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.IdempotencyClaimService;
import br.com.bb.transacoes.service.TransferenciaService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    /**
     * Mock do IdempotencyClaimService: isola o INSERT nativo (PostgreSQL) do
     * ambiente de PanacheMock. Sem este mock, Transferencia.getEntityManager()
     * retornaria null e todos os testes quebrariam com NPE antes de chegar
     * qualquer lógica de negócio.
     */
    @InjectMock
    IdempotencyClaimService idempotencyClaimService;

    @BeforeEach
    @Transactional
    void setupGlobalMocks() {
        OutboxEvent.deleteAll();

        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);

        // Padrão: chave nova → prosseguir com a transferência
        when(idempotencyClaimService.tryClaimKey(anyString())).thenReturn(true);

        // Padrão: findByIdempotencyKey retorna um objeto real para que o serviço
        // consiga preencher os campos (etapa 4 do fluxo) sem NPE
        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(new Transferencia());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Deve transferir com sucesso e persistir no Outbox")
    public void deveTransferirComSucesso() {
        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();
        mockContas(origem, destino);

        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(
                CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00")
        );

        service.realizarTransferencia(dto);

        Assertions.assertEquals(1, OutboxEvent.count(), "O evento de outbox deveria ter sido persistido");
        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
    }

    @Test
    @TestSecurity(user = "HACKER-ID", roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Deve falhar por Acesso Negado")
    public void deveFalharAcessoNegado() {
        mockContas(TestDataFactory.contaPadraoOrigem(), TestDataFactory.contaPadraoDestino());

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("50.00")))
        );

        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve falhar quando o saldo é insuficiente")
    public void deveFalharSaldoInsuficiente() {
        Conta origem = TestDataFactory.novaConta(CONTA_ORIGEM, USER_ID, new BigDecimal("50.00"));
        mockContas(origem, TestDataFactory.contaPadraoDestino());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00")))
        );

        Assertions.assertEquals("Saldo insuficiente para a operação.", exception.getMessage());
        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharOrigemInexistente() {
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10.00")))
        );
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve falhar quando a conta de destino não existe")
    public void deveFalharDestinoInexistente() {
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(TestDataFactory.contaPadraoOrigem());
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("10")))
        );
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Deve ignorar transferência se a chave de idempotência já existir")
    public void deveGarantirIdempotenciaNoService() {
        // Simula: chave já reservada no banco (INSERT retornou 0 rows)
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));
        when(idempotencyClaimService.tryClaimKey(dto.idempotencyKey())).thenReturn(false);

        boolean nova = service.realizarTransferencia(dto);

        Assertions.assertFalse(nova, "Deve retornar false para indicar duplicata");
        // Nenhuma operação financeira deve ter sido executada
        PanacheMock.verify(Conta.class, never()).findByNumeroWithLock(anyString());
        Assertions.assertTrue(connector.sink("transferencias-concluidas").received().isEmpty());
    }

    private void mockContas(Conta origem, Conta destino) {
        when(Conta.findByNumeroWithLock(origem.numero)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(destino.numero)).thenReturn(destino);
    }
}

