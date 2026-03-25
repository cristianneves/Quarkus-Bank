-- V1.2.0__Adicionar_Correlation_Id_Outbox.sql
-- Adiciona a coluna obrigatória para rastreabilidade
ALTER TABLE outbox_event ADD COLUMN correlation_id VARCHAR(50) NOT NULL DEFAULT 'sistema';
