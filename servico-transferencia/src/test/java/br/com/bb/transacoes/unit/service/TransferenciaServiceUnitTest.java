package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.TransferenciaService;
import br.com.bb.transacoes.unit.base.BaseUnitTest;
import br.com.bb.transacoes.base.TestDataFactory;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity; // 🚀 Importante
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.smallrye.mutiny.operators.multi.builders.NeverMulti.never;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TransferenciaServiceUnitTest extends BaseUnitTest {

    @Inject
    TransferenciaService service;

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    void setup() {
        connector.sink("transferencias-concluidas").clear();
    }

    @BeforeEach
    void setupMocks() {
        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) }) // 🔑 Garante o Principal Name
    @DisplayName("Deve realizar transferencia com sucesso logicamente")
    void deveTransferirComSucesso() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"), UUID.randomUUID().toString());

        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        service.realizarTransferencia(dto);

        assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
        validarMensagemNoKafka(1);
    }

    @Test
    @TestSecurity(user = "OUTRO-USUARIO", roles = "user") // 🛡️ Simula ID diferente do banco
    @DisplayName("Deve falhar por seguranca quando o dono da conta e diferente")
    void deveFalharAcessoNegado() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"), UUID.randomUUID().toString());

        Conta origem = TestDataFactory.contaPadraoOrigem();
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);

        assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        validarMensagemNoKafka(0);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("Unit: Deve falhar se uma das contas não existir")
    void deveFalharSeContaNaoExiste() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(null);
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(null); // Origem não existe

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertEquals("Conta de origem ou destino não encontrada.", ex.getMessage());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Unit: Deve falhar por saldo insuficiente")
    void deveFalharSaldoInsuficiente() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("2000.00"));

        Conta origem = TestDataFactory.contaPadraoOrigem(); // Tem 1000.00
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(null);
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertEquals("Saldo insuficiente.", ex.getMessage());
    }

    private void validarMensagemNoKafka(int esperada) {
        InMemorySink<TransferenciaDTO> sink = connector.sink("transferencias-concluidas");
        assertEquals(esperada, sink.received().size());
    }
}