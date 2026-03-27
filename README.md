# 🏦 Quarkus-Bank - Senior Core Banking Prototype

Este é um sistema de back-end bancário robusto, construído com **Quarkus**, focado em padrões de arquitetura corporativa, resiliência e integridade de dados.

## 🚀 Arquitetura e Diferenciais Técnicos
- **Transactional Outbox Pattern:** Garante consistência eventual entre o Banco de Dados e o Kafka, evitando o problema de "Dual Write".
- **Resiliência (Fault Tolerance):** Implementação de **Circuit Breaker** e **Fallback** em chamadas síncronas entre serviços.
- **Segurança (OIDC/Keycloak):** Autenticação e Autorização centralizadas via Keycloak com validação de Tokens JWT.
- **Mensageria Event-Driven:** Comunicação assíncrona para sincronização de contas via Kafka.
- **Locking Pessimista:** Proteção contra condições de corrida em transações financeiras críticas.

## 🏗️ Estrutura do Projeto
- `servico-cadastro`: Gestão de usuários, integração com Keycloak e sincronização de perfil.
- `servico-transferencia`: Núcleo financeiro, gestão de contas, saldos e processamento de transferências.
- `kong`: API Gateway para unificação dos endpoints.

## 🛠️ Tecnologias
- **Java 21 / Quarkus 3.x**
- **PostgreSQL 17** (Bancos isolados por serviço)
- **Apache Kafka (Redpanda)**
- **Keycloak 26**
- **Kong Gateway**

## 🏃 Como Rodar
1. Certifique-se de ter o Docker instalado.
2. Na raiz, execute:
   ```bash
   docker-compose up -d
   ```
3. O Keycloak estará disponível em `localhost:8180`, o Cadastro em `8082` e as Transferências em `8083`.
