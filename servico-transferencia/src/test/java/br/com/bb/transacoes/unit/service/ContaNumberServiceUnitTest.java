package br.com.bb.transacoes.unit.service;

import br.com.bb.transacoes.service.ContaNumberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o algoritmo de dígito verificador Módulo 10 (FEBRABAN).
 *
 * Não precisam de banco de dados nem de CDI — o método {@code calcularDigitoMod10}
 * é {@code public static} e puramente determinístico (sem efeitos colaterais).
 */
class ContaNumberServiceUnitTest {

    // ── Casos conhecidos (calculados manualmente) ──────────────────────────────

    @ParameterizedTest(name = "base={0} → DV esperado={1}")
    @CsvSource({
            // base,    DV  -- calculados pelo algoritmo módulo 10 FEBRABAN:
            //              mult 2,1 da direita; prod>=10 → soma algarismos; DV=(10-soma%10)%10
            "100000,   9",  // soma=1  → DV=9
            "100001,   7",  // soma=3  → DV=7
            "100002,   5",  // soma=5  → DV=5
            "100003,   3",  // soma=7  → DV=3
            "100004,   1",  // soma=9  → DV=1
            "100010,   8",  // soma=2  → DV=8
            "100100,   7",  // soma=3  → DV=7
            "999999,   6",  // soma=54 → DV=6
            "123456,   6",  // soma=24 → DV=6
            "111111,   1",  // soma=9  → DV=1
    })
    @DisplayName("Módulo 10: dígito verificador deve ser determinístico")
    void digitoVerificadorDeterministico(long base, int dvEsperado) {
        assertEquals(dvEsperado, ContaNumberService.calcularDigitoMod10(base),
                () -> "DV incorreto para base " + base);
    }

    @Test
    @DisplayName("DV deve estar sempre no intervalo [0,9]")
    void digitoSempreNoIntervalo() {
        // Percorre os primeiros 2.000 valores da sequência (base 100000+)
        for (long base = 100_000; base < 102_000; base++) {
            int dv = ContaNumberService.calcularDigitoMod10(base);
            assertTrue(dv >= 0 && dv <= 9,
                    "DV fora do intervalo para base " + base + ": " + dv);
        }
    }

    @Test
    @DisplayName("Números consecutivos da sequência devem gerar DVs diferentes com alta entropia")
    void numerosConsecutivosNaoDevemTerMesmoDV() {
        // Verifica que nem todos os 10 primeiros números têm o mesmo DV
        long[] distinctDvs = java.util.stream.LongStream.range(100_000, 100_010)
                .map(ContaNumberService::calcularDigitoMod10)
                .distinct()
                .toArray();
        assertTrue(distinctDvs.length > 1,
                "Todos os 10 números consecutivos geraram o mesmo DV — algoritmo suspeito");
    }

    @Test
    @DisplayName("Formato final deve corresponder a XXXXXX-D")
    void formatoDeveCorresponderAoPadrao() {
        // Simula a formatação usada em proximoNumeroConta() sem chamar o banco
        long base = 100001L;
        int dv = ContaNumberService.calcularDigitoMod10(base);
        String numero = String.format("%06d-%d", base, dv);

        assertTrue(numero.matches("\\d{6}-\\d"),
                "Formato inválido: " + numero);
    }
}



