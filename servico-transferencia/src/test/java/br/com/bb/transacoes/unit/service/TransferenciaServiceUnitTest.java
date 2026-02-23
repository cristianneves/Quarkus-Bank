package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.dto.TransferenciaDTO;
import br.com.bb.transacoes.exception.BusinessException;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.service.TransferenciaService;
import br.com.bb.transacoes.unit.base.BaseUnitTest;
import br.com.bb.transacoes.base.TestDataFactory;
import io.quarkus.test.junit.QuarkusTest;
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

    @Test
    @DisplayName("Deve realizar transferencia com sucesso logicamente")
    void deveTransferirComSucesso() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"), UUID.randomUUID().toString());

        // 🛡️ Uso elegante do Object Mother
        Conta origem = TestDataFactory.contaPadraoOrigem();
        Conta destino = TestDataFactory.contaPadraoDestino();

        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);
        when(Conta.findByNumeroWithLock(CONTA_DESTINO)).thenReturn(destino);

        service.realizarTransferencia(dto);

        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(origem.saldo));
        validarMensagemNoKafka(1);
    }

    @Test
    @DisplayName("Deve falhar por seguranca quando o dono da conta e diferente")
    void deveFalharAcessoNegado() {
        TransferenciaDTO dto = new TransferenciaDTO(CONTA_ORIGEM, CONTA_DESTINO, new BigDecimal("100.00"), UUID.randomUUID().toString());

        Conta origem = TestDataFactory.contaPadraoOrigem();
        when(Conta.findByNumeroWithLock(CONTA_ORIGEM)).thenReturn(origem);

        Assertions.assertThrows(BusinessException.class, () -> service.realizarTransferencia(dto));
        validarMensagemNoKafka(0);
    }

    private void validarMensagemNoKafka(int esperada) {
        InMemorySink<TransferenciaDTO> sink = connector.sink("transferencias-concluidas");
        Assertions.assertEquals(esperada, sink.received().size());
    }
}