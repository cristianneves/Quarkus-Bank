# 💰 Serviço de Transferência - Quarkus-Bank

O core financeiro do sistema, responsável pela movimentação de saldos e integridade transacional.

## 🚀 Funcionalidades Principais
- **Sincronia de Contas:** Escuta o tópico `v2-pessoa-registrada` do Kafka e cria automaticamente uma conta zerada para cada nova pessoa.
- **Transferências:** Processamento de transferências entre contas com **Locking Pessimista** para evitar race conditions.
- **Idempotência:** Validação de `idempotencyKey` para evitar transações duplicadas em reprocessamentos.
- **Auditoria:** Gravação de transações finalizadas no Kafka para sistemas downstream.

## 🛠️ Stack Técnica
- Quarkus REST
- SmallRye Kafka Connector
- Hibernate ORM with Panache
- LockModeType.PESSIMISTIC_WRITE
- Flyway (DB Versioning)

## 📡 Endpoints
- `GET /api/contas/detalhes/{keycloakId}`: Detalhes básicos da conta.
- `GET /api/contas/saldo/{keycloakId}`: Consulta rápida de saldo.
- `POST /transferencias`: Processa nova transferência financeira.
