-- V1.3.1__faxina_final.sql

-- Remove as colunas que não existem mais no seu código Java
ALTER TABLE outbox_event DROP COLUMN IF EXISTS eventtype;
ALTER TABLE outbox_event DROP COLUMN IF EXISTS event_type;

-- Garante que a coluna 'type' (que você quer usar) está correta
ALTER TABLE outbox_event ALTER COLUMN type SET NOT NULL;