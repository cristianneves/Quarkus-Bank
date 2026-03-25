package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.OutboxEvent;
import br.com.bb.transacoes.model.Transferencia;
import br.com.bb.transacoes.service.TransferenciaService;
import br.com.bb.transacoes.unit.base.BaseUnitTest;
import br.com.bb.transacoes.base.TestDataFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class TransferenciaServiceUnitTest extends BaseUnitTest {

    @Inject
    TransferenciaService service;

    @InjectMock
    SecurityIdentity identity;

    @BeforeEach
    @Transactional
    void setupDatabase() {
        OutboxEvent.deleteAll();
        Transferencia.deleteAll();
        Conta.deleteAll();

        TestDataFactory.contaPadraoOrigem().persist();
        TestDataFactory.contaPadraoDestino().persist();

        Principal p = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(p);
        lenient().when(p.getName()).thenReturn(USER_ID);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("1. Deve transferir com sucesso")
    void deveTransferirComSucesso() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));
        service.realizarTransferencia(dto);
        assertEquals(1, Transferencia.count());
        assertEquals(1, OutboxEvent.count());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("2. Deve permitir transferência com saldo igual ao valor")
    void devePermitirSaldoIgualAoValor() {
        BigDecimal valor = new BigDecimal("1000.00");
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, valor);
        service.realizarTransferencia(dto);
        assertEquals(0, BigDecimal.ZERO.compareTo(Conta.findByNumero(CONTA_ORIGEM).saldo));
    }

    @Test
    @TestSecurity(user = "HACKER", roles = "user")
    @DisplayName("4. Bloquear transferência de conta alheia")
    void deveFalharAcessoNegado() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);
        Principal p = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(p);
        when(p.getName()).thenReturn("HACKER_ID");
        assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
    }

    @Test
    @DisplayName("10. Garantir idempotência")
    @Transactional
    void deveTestarIdempotencia() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.ONE);
        Transferencia t = new Transferencia();
        t.idempotencyKey = dto.idempotencyKey();
        t.persist();
        service.realizarTransferencia(dto);
        assertEquals(1, Transferencia.count());
        assertEquals(0, OutboxEvent.count());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Service: Deve gerar correlationId se o MDC estiver vazio")
    void deveGerarCidSeMdcVazio() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);
        org.slf4j.MDC.remove("correlationId");
        service.realizarTransferencia(dto);
        OutboxEvent event = OutboxEvent.find("aggregateId", dto.idempotencyKey()).firstResult();
        assertNotNull(event.correlationId);
        assertTrue(event.correlationId.startsWith("test-"));
    }
}