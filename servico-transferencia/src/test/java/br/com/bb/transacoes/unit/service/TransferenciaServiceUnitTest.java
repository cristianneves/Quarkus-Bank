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
import io.quarkus.test.InjectMock; // 🎯 Necessário para o @InjectMock
import io.quarkus.test.security.TestSecurity;
import io.quarkus.security.identity.SecurityIdentity; // 🛡️ O tipo do Identity
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
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

// 🎯 CORREÇÃO DO IMPORT: Use Mockito.never, NÃO Mutiny.never
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TransferenciaServiceUnitTest extends BaseUnitTest {

    @Inject
    TransferenciaService service;

    @InjectMock // 🛡️ Isso substitui o SecurityIdentity real dentro do Service por um Mock
    SecurityIdentity identity;

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

        // 🛡️ Blindagem da Identidade (Evita o NPE que aciona o Fallback)
        Principal mockPrincipal = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(mockPrincipal);
        lenient().when(mockPrincipal.getName()).thenReturn(USER_ID);

        // 🛡️ Garante que a Idempotência retorne NULL por padrão (Caminho Feliz)
        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(null);
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

        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo),
                "O saldo deveria ser 900.00 mas foi " + origem.saldo);
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

    @Test
    @DisplayName("Fallback: Deve atualizar status para ERRO_ENVIO_KAFKA se o registro existir")
    void deveExecutarFallbackComSucesso() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO("123", "456", BigDecimal.TEN);
        Transferencia t = new Transferencia();
        t.idempotencyKey = dto.idempotencyKey();

        when(Transferencia.findByIdempotencyKey(dto.idempotencyKey())).thenReturn(t);

        service.fallbackTransferencia(dto);

        assertEquals("ERRO_ENVIO_KAFKA", t.status);
    }

    @Test
    @DisplayName("Unit: Deve ignorar transferência já processada (Idempotência)")
    void deveTestarIdempotencia() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO("1", "2", BigDecimal.ONE);
        when(Transferencia.findByIdempotencyKey(anyString())).thenReturn(new Transferencia());

        service.realizarTransferencia(dto);

        PanacheMock.verify(Conta.class, never()).findByNumeroWithLock(anyString());
    }

    @Test
    @TestSecurity(user = "qualquer", roles = "user")
    @DisplayName("Unit: Deve falhar quando o callerId for nulo")
    void deveFalharSeCallerIdNulo() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.ONE);
        Conta origem = TestDataFactory.contaPadraoOrigem();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(identity.getPrincipal()).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
    }

    @Test
    @DisplayName("Fallback: Deve criar registro de contingência quando não existir no banco")
    void deveCriarRegistroContingenciaQuandoNaoExiste() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO("111", "222", BigDecimal.TEN);

        when(Transferencia.findByIdempotencyKey(dto.idempotencyKey())).thenReturn(null);

        service.fallbackTransferencia(dto);

        PanacheMock.verify(Transferencia.class, times(1))
                .findByIdempotencyKey(dto.idempotencyKey());
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Unit: Deve executar fluxo feliz completo")
    void deveExecutarFluxoFelizCompleto() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        service.realizarTransferencia(dto);

        assertEquals(0, new BigDecimal("990.00").compareTo(origem.saldo));

        validarMensagemNoKafka(1);
    }

    @Test
    @TestSecurity(user = "OUTRO-USUARIO", roles = "user")
    @DisplayName("Unit: Deve bloquear acesso e não gerar efeitos colaterais")
    void deveBloquearAcessoENaoPersistir() {
        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, BigDecimal.TEN);

        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        Principal mockPrincipal = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn("OUTRO-USUARIO");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));

        assertEquals("Você não tem permissão para transferir desta conta.", ex.getMessage());

        validarMensagemNoKafka(0);
    }

    @Test
    @TestSecurity(user = USER_ID, roles = "user")
    @JwtSecurity(claims = { @Claim(key = "sub", value = USER_ID) })
    @DisplayName("Unit: Deve permitir transferência quando saldo é igual ao valor")
    void devePermitirSaldoIgualAoValor() {
        BigDecimal valor = new BigDecimal("1000.00");

        TransferenciaDTO dto = TestDataFactory.novaTransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, valor);

        Conta origem = TestDataFactory.novaConta(CONTA_ORIGEM, USER_ID, valor);
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        service.realizarTransferencia(dto);

        assertEquals(0, BigDecimal.ZERO.compareTo(origem.saldo));

        validarMensagemNoKafka(1);
    }


    private void validarMensagemNoKafka(int esperada) {
        InMemorySink<TransferenciaDTO> sink = connector.sink("transferencias-concluidas");
        assertEquals(esperada, sink.received().size());
    }
}