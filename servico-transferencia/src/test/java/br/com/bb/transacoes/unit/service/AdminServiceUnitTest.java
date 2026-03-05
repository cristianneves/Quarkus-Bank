package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.DepositoDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.AdminService;
import br.com.bb.transacoes.base.TestDataFactory;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AdminServiceUnitTest implements br.com.bb.transacoes.base.TestConstants {

    @Inject
    AdminService adminService;

    @BeforeEach
    void setup() {
        PanacheMock.mock(Conta.class);
    }

    @Test
    @DisplayName("Admin Unit: Deve realizar depósito com sucesso")
    void deveDepositarComSucesso() {
        Conta conta = TestDataFactory.contaPadraoOrigem();
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(conta);

        adminService.realizarDeposito(new DepositoDTO(CONTA_ORIGEM, new BigDecimal("100.00")));

        assertEquals(0, new BigDecimal("1100.00").compareTo(conta.saldo));
    }

    @Test
    @DisplayName("Admin Unit: Deve falhar para valor negativo ou zero")
    void deveFalharValorInvalido() {
        Conta conta = TestDataFactory.contaPadraoOrigem();
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(conta);

        assertThrows(BusinessException.class, () ->
                adminService.realizarDeposito(new DepositoDTO(CONTA_ORIGEM, BigDecimal.ZERO))
        );
    }
}