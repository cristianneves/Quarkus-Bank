-- Sequência dedicada para geração de números de conta bancária.
--
-- MOTIVAÇÃO:
--   A implementação anterior usava SecureRandom.nextInt(900_000) + 100_000
--   gerando um espaço de apenas 900.000 valores. Pelo paradoxo do aniversário,
--   a probabilidade de colisão com N contas é ≈ 1 - e^(-N²/1.800.000).
--   Com 1.000 contas a chance de colisão já é ~42%; com 1.500 contas ultrapassa 71%.
--   Colisão → UniqueConstraintViolationException → nack Kafka → retry storm.
--
-- SOLUÇÃO:
--   Sequência monotônica: PostgreSQL garante que cada chamada a nextval retorna
--   um valor único, atômico e sem risco de colisão, independente de concorrência.
--
-- FORMATO DO NÚMERO DE CONTA: XXXXXX-D
--   XXXXXX = valor da sequência (6 dígitos, base 100000)
--   D      = dígito verificador módulo 10 (padrão FEBRABAN)
--   Exemplo: 100001-3, 100002-7, 100003-0

CREATE SEQUENCE IF NOT EXISTS conta_numero_seq
    START WITH 100000
    INCREMENT BY 1
    MINVALUE 100000
    NO CYCLE;

