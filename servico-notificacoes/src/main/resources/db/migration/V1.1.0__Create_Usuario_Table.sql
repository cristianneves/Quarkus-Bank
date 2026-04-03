-- V1.1.0__Create_Usuario_Table.sql
CREATE TABLE usuario_notificacao (
    keycloak_id     VARCHAR(255) PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    nome            VARCHAR(255) NOT NULL
);

CREATE INDEX idx_usuario_email ON usuario_notificacao (email);
