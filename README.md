# Mini Digital Banking

Hệ thống ngân hàng số thu nhỏ theo kiến trúc **Microservices**, dùng làm dự án portfolio để thể hiện các kỹ thuật production-grade: Spring Boot, Kafka, OAuth2/Keycloak, PostgreSQL, Vault, ELK, Docker Compose và Resilience4j.

## Tính năng

- Quản lý tài khoản và số dư (`account-service`)
- Chuyển tiền giữa các tài khoản, lịch sử giao dịch (`transaction-service`)
- Thông báo sự kiện giao dịch (`notification-service`)
- Xác thực OAuth2/OIDC qua Keycloak với phân quyền theo role (CUSTOMER, TELLER, ADMIN)
- Giao tiếp bất đồng bộ qua Kafka (Transactional Outbox pattern)
- Circuit Breaker, Rate Limiter, Retry (Resilience4j)
- Quản lý secret tập trung với HashiCorp Vault (dev mode)
- Thu thập log qua Filebeat → Elasticsearch → Kibana

## Kiến trúc

```
Client (Postman / Swagger)
        │
        ▼
┌───────────────────┐     REST      ┌────────────────────┐
│ transaction-service│ ────────────► │  account-service   │
└─────────┬─────────┘               └─────────┬──────────┘
          │                                   │
          │         Kafka (events)              │
          └──────────────┬────────────────────┘
                         ▼
              ┌─────────────────────┐
              │ notification-service │
              └─────────────────────┘

Hạ tầng: PostgreSQL · Keycloak · Kafka · Vault · Elasticsearch · Kibana
```

## Cấu trúc project

```
├── account-service/        # Quản lý tài khoản, số dư
├── transaction-service/    # Xử lý chuyển tiền
├── notification-service/   # Thông báo giao dịch
├── common/                 # DTO, exception, Kafka topics dùng chung
├── infra/                  # Keycloak realm, Postgres init, Filebeat
├── docker-compose.yml      # Chạy toàn bộ stack local
└── *.yaml                  # OpenAPI specs cho từng service
```

## Yêu cầu

- Docker & Docker Compose
- Java 21+ (nếu build local không qua Docker)
- Gradle (hoặc dùng `./gradlew` có sẵn trong repo)

## Chạy local

```bash
docker compose up -d --build
```

| Service              | URL                        |
| -------------------- | -------------------------- |
| Keycloak             | http://localhost:8080      |
| Account Service      | http://localhost:8081      |
| Transaction Service  | http://localhost:8082      |
| Notification Service | http://localhost:8083      |
| Kafka UI             | http://localhost:8090      |
| Vault                | http://localhost:8200      |
| Kibana               | http://localhost:5601      |

**Keycloak (dev):** admin / admin  
**Realm:** `banking-demo`  
**Vault token (dev):** `root-token-dev-only`

### Lấy access token

```bash
curl -s -X POST "http://localhost:8080/realms/banking-demo/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=banking-api" \
  -d "username=customer1" \
  -d "password=password" | jq -r .access_token
```

Dùng token trong header: `Authorization: Bearer <token>`

## Build local (không Docker)

```bash
./gradlew build
./gradlew :account-service:bootRun
./gradlew :transaction-service:bootRun
./gradlew :notification-service:bootRun
```

Cần PostgreSQL, Kafka và Keycloak đang chạy (có thể chỉ start hạ tầng: `docker compose up -d postgres kafka keycloak`).

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Security OAuth2 Resource Server
- **Database:** PostgreSQL 16 (database-per-service)
- **Messaging:** Apache Kafka 3.7 (KRaft)
- **Identity:** Keycloak 25
- **Secrets:** HashiCorp Vault
- **Observability:** Elasticsearch, Kibana, Filebeat
- **Resilience:** Resilience4j

## Lưu ý

- Cấu hình trong repo dành cho **môi trường dev/demo** — không dùng trực tiếp cho production.
- File `.env` không được commit (xem `.gitignore`).

## License

Private project — all rights reserved.
