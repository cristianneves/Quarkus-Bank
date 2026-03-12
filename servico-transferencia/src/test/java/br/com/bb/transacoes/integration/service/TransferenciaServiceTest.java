package br.com.bb.transacoes.integration.service;

import br.com.bb.transacoes.base.TestDataFactory;
import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.integration.base.BaseMessagingTest;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.TransferenciaService;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity; // 🚀 Importante
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

    @BeforeEach
    @Transactional
    void setupGlobalMocks() {
        OutboxEvent.deleteAll();

        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);

        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(null);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Deve transferir com sucesso e persistir no Outbox")
    public void deveTransferirComSucesso() {
        // Arrange
        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();
        mockContas(origem, destino); // Assume que este método configura o when() do PanacheMock

        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(
                CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00")
        );

        // Act
        service.realizarTransferencia(dto);

        // Assert
        // Agora o count será sempre 1 porque limpamos no @BeforeEach
        Assertions.assertEquals(1, OutboxEvent.count(), "O evento de outbox deveria ter sido persistido");

        // Validamos a lógica de negócio no objeto mockado
        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
    }

    @Test
    @TestSecurity(user = "HACKER-ID", roles = "user") // 🛡️ Simula um invasor
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) }) // 🚀 E AQUI TAMBÉM!
    @DisplayName("Deve falhar por Acesso Negado")
    public void deveFalharAcessoNegado() {
        mockContas(TestDataFactory.contaPadraoOrigem(), TestDataFactory.contaPadraoDestino());

        Assertions.assertThrows(BusinessException.class, () ->
                service.realizarTransferencia(TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("50.00")))
        );

        PanacheMock.verify(Transferencia.class, times(0)).persist();
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user") // 🛡️ Identidade correta, mas saldo baixo
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