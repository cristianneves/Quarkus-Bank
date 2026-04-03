package br.com.bb.transacoes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Gera números de conta bancária únicos, válidos e auditáveis.
 *
 * <h2>Por que sequência e não SecureRandom?</h2>
 * <p>O gerador anterior produzia um inteiro aleatório no intervalo [100000, 999999]
 * (900.000 valores possíveis). Pelo paradoxo do aniversário, com apenas ~1.000 contas
 * abertas a probabilidade acumulada de colisão já supera 42 %. Uma colisão causa
 * {@code UniqueConstraintViolationException} → {@code nack} Kafka → retry em loop.</p>
 *
 * <p>A sequência PostgreSQL {@code conta_numero_seq} é monotônica, atômica e
 * gerenciada pelo próprio banco: cada chamada a {@code nextval} retorna um valor
 * único mesmo sob concorrência máxima, sem retry e sem exceções de constraint.</p>
 *
 * <h2>Formato do número de conta</h2>
 * <pre>
 *   XXXXXX-D
 *   XXXXXX = valor da sequência com 6 dígitos (ex: 100001)
 *   D      = dígito verificador módulo 10 (padrão FEBRABAN)
 *   Exemplo: "100001-3", "100002-7"
 * </pre>
 *
 * <p>O dígito verificador permite detectar erros de transcrição e é exigido pelo
 * padrão de compensação FEBRABAN para contas correntes brasileiras.</p>
 *
 * <p>Extraído de {@code ContaConsumer} para ser injetável e substituível por
 * {@code @InjectMock} nos testes de integração do consumer.</p>
 */
@ApplicationScoped
public class ContaNumberService {

    @Inject
    EntityManager em;

    /**
     * Retorna o próximo número de conta no formato {@code XXXXXX-D}.
     *
     * <p>Deve ser chamado dentro de uma transação ativa
     * ({@link Transactional.TxType#MANDATORY}).</p>
     *
     * @return número de conta único com dígito verificador, ex: {@code "100001-3"}
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public String proximoNumeroConta() {
        long base = ((Number) em
                .createNativeQuery("SELECT nextval('conta_numero_seq')")
                .getSingleResult())
                .longValue();

        int digito = calcularDigitoMod10(base);
        return String.format("%06d-%d", base, digito);
    }

    /**
     * Calcula o dígito verificador pelo algoritmo Módulo 10 (padrão FEBRABAN).
     *
     * <p>Regras:</p>
     * <ol>
     *   <li>Percorre os dígitos da direita para a esquerda.</li>
     *   <li>Multiplica alternadamente por 2 e por 1 (primeiro da direita: ×2).</li>
     *   <li>Se o produto for ≥ 10, soma os algarismos do produto (ex: 16 → 1+6 = 7).</li>
     *   <li>Soma todos os resultados.</li>
     *   <li>DV = (10 - soma % 10) % 10  →  se result = 10 então DV = 0.</li>
     * </ol>
     *
     * @param numero número base da sequência (positivo)
     * @return dígito verificador no intervalo [0, 9]
     */
    // public: algoritmo FEBRABAN é conhecimento público; clientes podem verificar
    //         seus próprios números de conta sem chamar o backend.
    public static int calcularDigitoMod10(long numero) {
        String digits = Long.toString(numero);
        int soma = 0;
        int multiplicador = 2; // começa com 2 na posição menos significativa
        for (int i = digits.length() - 1; i >= 0; i--) {
            int prod = Character.getNumericValue(digits.charAt(i)) * multiplicador;
            soma += (prod >= 10) ? (prod / 10) + (prod % 10) : prod;
            multiplicador = (multiplicador == 2) ? 1 : 2;
        }
        return (10 - soma % 10) % 10;
    }
}


