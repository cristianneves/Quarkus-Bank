CREATE TABLE outbox_event (
                              id BIGINT PRIMARY KEY,
                              aggregateType VARCHAR(50) NOT NULL, -- Ex: "TRANSFERENCIA"
                              aggregateId VARCHAR(255) NOT NULL,   -- O idempotencyKey ou ID da transferência
                              type VARCHAR(50) NOT NULL,          -- Ex: "TRANSFERENCIA_REALIZADA"
                              payload TEXT NOT NULL,              -- O JSON do DTO
                              createdAt TIMESTAMP NOT NULL,
                              processedAt TIMESTAMP               -- Nulo até ser enviado ao Kafka
);

CREATE SEQUENCE outbox_event_SEQ START WITH 1 INCREMENT BY 50;

-- Índice para o Worker não sofrer com o crescimento da tabela
CREATE INDEX idx_outbox_unprocessed ON outbox_event (createdAt) WHERE (processedAt IS NULL);