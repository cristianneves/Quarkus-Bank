package br.com.bb.transacoes.unit.base;

import br.com.bb.transacoes.base.TestConstants;
import br.com.bb.transacoes.model.Conta;
import br.com.bb.transacoes.model.Transferencia;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.when;

public abstract class BaseUnitTest implements TestConstants {


    @BeforeEach
    public void setupBase() {
        PanacheMock.mock(Conta.class);
        PanacheMock.mock(Transferencia.class);
    }
}