-- 1. Sequências com os nomes exatos que o Hibernate pediu no seu log
CREATE SEQUENCE IF NOT EXISTS Transferencia_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS Conta_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS audit_log_SEQ START WITH 1 INCREMENT BY 50;

-- 2. Sequência padrão para garantir
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

-- 3. Tabelas (Nomes em minúsculo são o padrão do Postgres)
CREATE TABLE IF NOT EXISTS audit_log (
                                         id BIGINT PRIMARY KEY,
                                         usuario VARCHAR(255),
    acao VARCHAR(255),
    detalhes TEXT,
    dataHora TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS conta (
                                     id BIGINT PRIMARY KEY,
                                     numero VARCHAR(255) NOT NULL UNIQUE,
    agencia VARCHAR(255) NOT NULL,
    saldo NUMERIC(19, 2) NOT NULL,
    keycloakId VARCHAR(255) NOT NULL UNIQUE,
    nomeTitular VARCHAR(255),
    cpfTitular VARCHAR(255) NOT NULL,
    emailTitular VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS transferencia (
                                             id BIGINT PRIMARY KEY,
                                             numeroOrigem VARCHAR(255),
    numeroDestino VARCHAR(255),
    valor NUMERIC(19, 2),
    dataHora TIMESTAMP,
    status VARCHAR(255),
    idempotencyKey VARCHAR(255) UNIQUE
    );