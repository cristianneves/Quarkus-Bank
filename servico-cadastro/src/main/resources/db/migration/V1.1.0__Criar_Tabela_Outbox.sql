CREATE TABLE outbox_event (
    id BIGINT NOT NULL,
    aggregateType VARCHAR(50) NOT NULL,
    aggregateId VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    processedAt TIMESTAMP,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

CREATE SEQUENCE outbox_event_seq START WITH 1 INCREMENT BY 50;

-- Índice para performance do Worker (busca apenas os não processados)
CREATE INDEX idx_outbox_unprocessed ON outbox_event (createdAt) WHERE (processedAt IS NULL);