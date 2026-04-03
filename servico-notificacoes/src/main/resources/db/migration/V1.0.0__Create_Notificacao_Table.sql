-- V1.0.0__Create_Notificacao_Table.sql
CREATE SEQUENCE IF NOT EXISTS notificacao_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE notificacao (
    id              BIGINT PRIMARY KEY,
    keycloak_id     VARCHAR(255) NOT NULL,      -- destinatário
    titulo          VARCHAR(255) NOT NULL,
    mensagem        TEXT         NOT NULL,
    tipo            VARCHAR(50)  NOT NULL,       -- BEM_VINDO | DEBITO | CREDITO | ENCERRAMENTO
    status          VARCHAR(20)  NOT NULL,       -- PENDENTE | ENVIADA | LIDA
    aggregate_id    VARCHAR(255) NOT NULL,       -- idempotencyKey ou keycloakId
    correlation_id  VARCHAR(255),
    criado_em       TIMESTAMP    NOT NULL,
    enviado_em      TIMESTAMP
);

CREATE INDEX idx_notificacao_keycloak ON notificacao (keycloak_id);
