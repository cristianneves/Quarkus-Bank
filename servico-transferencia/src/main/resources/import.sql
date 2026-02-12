-- Usamos 'keycloakId' em vez de 'titular' para bater com a sua nova Entity
INSERT INTO Conta (id, numero, agencia, saldo, keycloakId)
VALUES (nextval('Conta_SEQ'), '12345-6', '0001', 1000.00, 'user-origem-id');

INSERT INTO Conta (id, numero, agencia, saldo, keycloakId)
VALUES (nextval('Conta_SEQ'), '54321-0', '0001', 500.50, 'user-destino-id');