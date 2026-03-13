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
        // 🛡️ LIMPEZA: O banco de teste deve estar puro para cada execução
        OutboxEvent.deleteAll();
        Transferencia.deleteAll();
        Conta.deleteAll();

        // 🛡️ POPULAÇÃO: Criamos os registros reais no banco H2/Postgres de teste
        TestDataFactory.contaPadraoOrigem().persist();
        TestDataFactory.contaPadraoDestino().persist();

        // Mock da identidade para controle de segurança via Mockito
        Principal p = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(p);
        lenient().when(p.getName()).thenReturn(USER_ID);
    }

    // --- 1. SUCESSO: FLUXO FELIZ ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("1. Deve transferir com sucesso e persistir no banco e no Outbox")
    void deveTransferirComSucesso() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"));

        service.realizarTransferencia(dto);

        assertEquals(1, Transferencia.count());
        assertEquals(1, OutboxEvent.count());
    }

    // --- 2. SUCESSO: SALDO LIMITE ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("2. Deve permitir transferência com saldo exatamente igual ao valor")
    void devePermitirSaldoIgualAoValor() {
        BigDecimal valor = new BigDecimal("1000.00");
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, valor);

        service.realizarTransferencia(dto);

        assertEquals(0, BigDecimal.ZERO.compareTo(Conta.findByNumero(CONTA_ORIGEM).saldo));
    }

    // --- 3. SUCESSO: VALIDAÇÃO DE AMBOS OS SALDOS ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("3. Deve validar que os saldos de origem e destino foram alterados corretamente")
    void deveValidarAlteracaoDeAmbosOsSaldos() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("200.00"));

        service.realizarTransferencia(dto);

        assertEquals(0, new BigDecimal("800.00").compareTo(Conta.findByNumero(CONTA_ORIGEM).saldo));
        assertEquals(0, new BigDecimal("700.00").compareTo(Conta.findByNumero(CONTA_DESTINO).saldo));
    }

    // --- 4. SEGURANÇA: USUÁRIO ERRADO ---
    @Test
    @TestSecurity(user = "HACKER", roles = "user")
    @DisplayName("4. Deve impedir transferência de conta que não pertence ao usuário logado")
    void deveFalharAcessoNegado() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        Principal p = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(p);
        when(p.getName()).thenReturn("HACKER_ID");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertEquals("Operação não autorizada para este usuário.", ex.getMessage());
    }

    // --- 5. SEGURANÇA: PRINCIPAL NULO ---
    @Test
    @TestSecurity(user = "qualquer", roles = "user")
    @DisplayName("5. Deve falhar quando o Principal Name não for localizado")
    void deveFalharSeCallerIdNulo() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.ONE);

        // Forçamos o cenário de ausência de principal
        when(identity.getPrincipal()).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));

        // Validamos se a mensagem condiz com o novo check defensivo
        assertTrue(ex.getMessage().contains("Usuário não identificado"));
    }

    // --- 6. NEGÓCIO: SALDO INSUFICIENTE ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("6. Deve falhar quando não houver saldo suficiente")
    void deveFalharSaldoInsuficiente() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("5000.00"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertEquals("Saldo insuficiente para a operação.", ex.getMessage());
    }

    // --- 7. NEGÓCIO: CONTA ORIGEM INEXISTENTE ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("7. Deve falhar se a conta de origem não existir no banco")
    void deveFalharSeContaNaoExiste() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO("999999", CONTA_DESTINO, BigDecimal.TEN);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertTrue(ex.getMessage().contains("não localizada"));
    }

    // --- 8. NEGÓCIO: CONTA DESTINO INEXISTENTE ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @DisplayName("8. Deve falhar se a conta de destino não existir no banco")
    void deveFalharSeContaDestinoNaoExiste() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, "888888", BigDecimal.TEN);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertTrue(ex.getMessage().contains("não localizada"));
    }

    // --- 9. DOMÍNIO: VALOR INVÁLIDO ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("9. Deve falhar para valores de transferência negativos ou zero")
    void deveFalharParaValorInvalido() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("-5.00"));

        assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
    }

    // --- 10. RESILIÊNCIA: IDEMPOTÊNCIA ---
    @Test
    @DisplayName("10. Deve garantir idempotência se a chave já tiver sido processada")
    @Transactional
    void deveTestarIdempotencia() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.ONE);

        Transferencia t = new Transferencia();
        t.idempotencyKey = dto.idempotencyKey();
        t.persist(); // Já existe uma transferência com essa chave

        service.realizarTransferencia(dto);

        assertEquals(1, Transferencia.count()); // Não criou a segunda
        assertEquals(0, OutboxEvent.count());   // Não gerou evento duplicado
    }

    // --- 11. INTEGRIDADE: CONTAGEM DE REGISTROS ---
    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("11. Deve garantir que o registro de transferência e outbox foram criados")
    void deveExecutarFluxoFelizCompleto() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        service.realizarTransferencia(dto);

        assertEquals(1, Transferencia.count());
        assertEquals(1, OutboxEvent.count());
    }

    // --- 12. SEGURANÇA: MENSAGEM ESPECÍFICA ---
    @Test
    @TestSecurity(user = "OUTRO-USUARIO", roles = "user")
    @DisplayName("12. Deve retornar a mensagem de erro correta em caso de fraude")
    void deveBloquearAcessoENaoPersistir() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        Principal mockPrincipal = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("OUTRO-USUARIO");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        assertEquals("Operação não autorizada para este usuário.", ex.getMessage());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Service: Deve salvar o Correlation ID no OutboxEvent")
    void deveSalvarCorrelationIdNoOutbox() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        // Simulamos um ID já existente no MDC (como se tivesse vindo do Filtro)
        String cidOriginal = "meu-id-rastreavel";
        org.slf4j.MDC.put("correlationId", cidOriginal);

        service.realizarTransferencia(dto);

        OutboxEvent event = OutboxEvent.find("aggregateId", dto.idempotencyKey()).firstResult();
        assertNotNull(event.correlationId);
        assertEquals(cidOriginal, event.correlationId, "O ID no banco deve ser o mesmo do MDC");
    }
}