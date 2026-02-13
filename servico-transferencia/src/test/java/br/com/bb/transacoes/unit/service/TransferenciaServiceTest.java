package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferenciaServiceTest {

    @Mock
    Emitter<TransferenciaDTO> emissorTransferencia;

    @InjectMocks
    TransferenciaService service;

    @Test
    @DisplayName("Deve realizar transferência e atualizar saldos corretamente")
    public void deveRealizarTransferenciaComSucesso() {
        try (MockedStatic<Conta> contaMock = mockStatic(Conta.class)) {
            // 1. GIVEN: Contas com saldo suficiente
            Conta origem = new Conta("123", new BigDecimal("1000.00"));
            Conta destino = new Conta("456", new BigDecimal("500.00"));
            TransferenciaDTO dto = new TransferenciaDTO("123", "456", new BigDecimal("200.00"));

            contaMock.when(() -> Conta.findByNumero("123")).thenReturn(origem);
            contaMock.when(() -> Conta.findByNumero("456")).thenReturn(destino);

            // 2. WHEN
            service.realizarTransferencia(dto);

            // 3. THEN: Validação da matemática bancária
            // 1000 - 200 = 800
            assertEquals(0, new BigDecimal("800.00").compareTo(origem.saldo));
            // 500 + 200 = 700
            assertEquals(0, new BigDecimal("700.00").compareTo(destino.saldo));

            // VERIFY: Garante que o evento foi enviado ao Kafka
            verify(emissorTransferencia, times(1)).send(any(TransferenciaDTO.class));
        }
    }

    @Test
    @DisplayName("Deve falhar quando a conta de origem não existe")
    public void deveFalharContaNaoEncontrada() {
        try (MockedStatic<Conta> contaMock = mockStatic(Conta.class)) {
            // 1. GIVEN: Uma conta nula
            TransferenciaDTO dto = new TransferenciaDTO("999", "456", new BigDecimal("10.00"));
            contaMock.when(() -> Conta.findByNumero("999")).thenReturn(null);

            // 2. WHEN & THEN
            BusinessException ex = assertThrows(BusinessException.class, () -> {
                service.realizarTransferencia(dto);
            });

            assertEquals("Conta nao encontrada", ex.getMessage());
            verify(emissorTransferencia, never()).send(any(TransferenciaDTO.class));
        }
    }
}