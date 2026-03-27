# 📄 Serviço de Cadastro - Quarkus-Bank

Gerencia o ciclo de vida dos usuários e integração com o Identity Provider (Keycloak).

## 🚀 Funcionalidades Principais
- **Registro de Usuários:** Criação de conta no Keycloak e persistência local sincronizada.
- **Transactional Outbox:** Garante a emissão confiável do evento `PESSOA_CRIADA` para o Kafka.
- **Resiliência (Perfil):** Consulta de saldo no `servico-transferencia` via REST Client com **Circuit Breaker** e **Fallback** em caso de indisponibilidade.
- **Segurança:** Validação de Token JWT e Roles baseadas no Keycloak.

## 🛠️ Stack Técnica
- Quarkus REST (RESTEasy Reactive)
- Hibernate ORM with Panache (Active Record)
- SmallRye Fault Tolerance (Circuit Breaker)
- Keycloak Admin Client
- Flyway (DB Versioning)

## 📡 Endpoints
- `POST /api/pessoas/registrar`: Registro aberto de novos usuários.
- `GET /api/perfil`: Consulta de perfil do usuário logado (exige JWT).
- `DELETE /api/pessoas/{email}`: Exclusão lógica com compensação no Keycloak.
