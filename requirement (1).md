# Requirement Document

## Digital Banking Microservices Platform (Demo Project)

**Mục tiêu tài liệu:** Đặc tả yêu cầu cho một hệ thống banking thu nhỏ, đóng vai trò là dự án cá nhân (portfolio project) nhằm chứng minh khả năng vận hành thực tế các kỹ thuật: Microservices, Spring Boot, Kafka, Kubernetes, Docker Compose, CI/CD, Elasticsearch, Vault, Resilience4j, Keycloak/OAuth2, custom exception handling.

---

## 1. Tổng Quan Dự Án

### 1.1. Bối cảnh

Xây dựng một hệ thống ngân hàng số thu nhỏ mô phỏng nghiệp vụ cốt lõi: **quản lý tài khoản, chuyển tiền giữa các tài khoản, lịch sử giao dịch, và thông báo cho khách hàng**. Hệ thống được thiết kế theo kiến trúc Microservices, đủ nhỏ để một người triển khai trong 4–8 tuần, nhưng đủ đầy đủ để thể hiện các kỹ thuật production-grade.

### 1.2. Mục tiêu kỹ thuật

- Thiết kế và vận hành hệ thống Microservices thực tế, không chỉ demo lý thuyết.
- Áp dụng giao tiếp đồng bộ (REST) và bất đồng bộ (Kafka) giữa các service.
- Bảo mật theo chuẩn OAuth2/OIDC với Keycloak, phân quyền theo role trong JWT.
- Đảm bảo khả năng chịu lỗi (Resilience4j: Circuit Breaker, Rate Limiter, Retry).
- Quản lý secret tập trung bằng Vault, không hard-code credential.
- Thu thập, tập trung hoá và trực quan hoá log bằng ELK/EFK stack.
- Đóng gói bằng Docker, triển khai và quản lý bằng Kubernetes.
- Tự động hoá build – test – deploy bằng CI/CD pipeline.
- Chuẩn hoá xử lý lỗi bằng custom exception + error code.



### 1.3. Phạm vi (Scope)

**Trong phạm vi:**

- 3 microservices nghiệp vụ + Keycloak + hạ tầng hỗ trợ (Kafka, Vault, ELK, Kubernetes, CI/CD).
- Luồng nghiệp vụ: đăng ký/tra cứu tài khoản, chuyển tiền, xem lịch sử giao dịch, nhận thông báo.

**Ngoài phạm vi (không cần làm để tiết kiệm thời gian):**

- Tích hợp core banking thật hoặc cổng thanh toán thật.
- Giao diện người dùng phức tạp (chỉ cần Postman collection / Swagger UI là đủ).
- Xử lý đa tiền tệ, lãi suất, KYC/AML thật.

---



## 2. Kiến Trúc Hệ Thống



### 2.1. Danh sách services


| Service                  | Vai trò                                                   | Giao tiếp đồng bộ                                        | Giao tiếp bất đồng bộ (Kafka)                                                 |
| ------------------------ | --------------------------------------------------------- | -------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **account-service**      | Quản lý tài khoản, số dư, chủ tài khoản                   | REST API (được gọi bởi transaction-service, client)      | Publish: `account.balance.updated`; Consume: `transaction.completed`          |
| **transaction-service**  | Xử lý lệnh chuyển tiền, ghi lịch sử giao dịch             | Gọi REST đến account-service (kiểm tra & cập nhật số dư) | Publish: `transaction.created`, `transaction.completed`, `transaction.failed` |
| **notification-service** | Gửi thông báo (giả lập email/SMS bằng log) cho khách hàng | Không expose API nghiệp vụ, chỉ có health/actuator       | Consume: `transaction.completed`, `transaction.failed`                        |




### 2.2. Hạ tầng hỗ trợ (Infrastructure)


| Thành phần                                           | Vai trò                                                           |
| ---------------------------------------------------- | ----------------------------------------------------------------- |
| **Keycloak**                                         | Identity Provider — OAuth2/OIDC, phát hành JWT, quản lý user/role |
| **Kafka + Zookeeper (hoặc KRaft)**                   | Message broker cho giao tiếp bất đồng bộ giữa các service         |
| **PostgreSQL (hoặc Oracle)**                         | Mỗi service có schema/database riêng (Database-per-service)       |
| **Vault**                                            | Quản lý secret tập trung: DB credentials, Keycloak client secret  |
| **Elasticsearch + Logstash/Filebeat + Kibana (EFK)** | Thu thập, lưu trữ, tra cứu và trực quan hoá log                   |
| **Redis** *(mở rộng)*                                | Cache số dư tài khoản để giảm tải cho account-service             |
| **Kubernetes**                                       | Điều phối container, tự phục hồi (self-healing), autoscaling      |




### 2.3. Sơ đồ luồng nghiệp vụ chính (mô tả text)

```
Client (Postman/UI)
   │  1. Login → nhận Access Token (JWT) từ Keycloak
   ▼
[API Gateway / Ingress] ── xác thực token, forward request
   │
   ▼
transaction-service  ──(REST, có Circuit Breaker)──▶  account-service
   │                                                        │
   │ 2. Publish "transaction.created"                       │ 3. Trừ/cộng số dư,
   ▼                                                         │    publish "account.balance.updated"
 Kafka Topic: transaction.created                            
   │
   ▼
transaction-service xử lý kết quả → publish "transaction.completed" / "transaction.failed"
   │
   ▼
notification-service consume → ghi log giả lập gửi email/SMS
```



### 2.4. Nguyên tắc thiết kế

- **Database per service**: mỗi service sở hữu database riêng, không truy cập chéo database.
- **API-led connectivity**: mọi giao tiếp đồng bộ đi qua REST API có versioning (`/api/v1/...`).
- **Event-driven cho nghiệp vụ không cần phản hồi ngay**: cập nhật số dư, gửi thông báo dùng Kafka thay vì gọi đồng bộ để giảm coupling.
- **Idempotency**: các consumer Kafka phải xử lý idempotent (dùng `transactionId` làm khoá chống xử lý trùng khi message bị retry).

---



## 3. Yêu Cầu Chức Năng (Functional Requirements)


| ID    | Mô tả                                                                                                                 | Service                               |
| ----- | --------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| FR-01 | Tạo tài khoản khách hàng mới với số dư khởi tạo                                                                       | account-service                       |
| FR-02 | Tra cứu thông tin và số dư tài khoản theo `accountId`                                                                 | account-service                       |
| FR-03 | Khởi tạo lệnh chuyển tiền giữa 2 tài khoản (`fromAccount`, `toAccount`, `amount`)                                     | transaction-service                   |
| FR-04 | Kiểm tra số dư đủ điều kiện trước khi trừ tiền (validate nghiệp vụ)                                                   | transaction-service ↔ account-service |
| FR-05 | Ghi nhận và cập nhật trạng thái giao dịch: `PENDING` → `COMPLETED` / `FAILED`                                         | transaction-service                   |
| FR-06 | Truy vấn lịch sử giao dịch theo tài khoản, có phân trang                                                              | transaction-service                   |
| FR-07 | Gửi thông báo khi giao dịch hoàn tất hoặc thất bại                                                                    | notification-service                  |
| FR-08 | Người dùng phải đăng nhập qua Keycloak để lấy access token trước khi gọi API                                          | Keycloak                              |
| FR-09 | Phân quyền: `CUSTOMER` chỉ thao tác trên tài khoản của mình; `TELLER` xem được nhiều tài khoản; `ADMIN` có toàn quyền | Tất cả service                        |
| FR-10 | Mọi lỗi nghiệp vụ trả về response chuẩn hoá kèm mã lỗi cụ thể                                                         | Tất cả service                        |


---



## 4. Yêu Cầu Phi Chức Năng (Non-Functional Requirements)


| Nhóm                | Yêu cầu                                                                                                                                   |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| **Availability**    | Mỗi service chạy tối thiểu 2 replicas trên Kubernetes; Kubernetes tự khởi động lại pod lỗi (liveness/readiness probe)                     |
| **Scalability**     | Cấu hình Horizontal Pod Autoscaler (HPA) dựa trên CPU cho transaction-service                                                             |
| **Fault Tolerance** | Circuit Breaker + Retry + Rate Limiter (Resilience4j) cho lời gọi transaction-service → account-service                                   |
| **Security**        | Toàn bộ API (trừ endpoint public/health) yêu cầu Bearer token hợp lệ từ Keycloak; secret không lưu plain-text                             |
| **Observability**   | Log dạng JSON có `traceId`/`correlationId`; log được đẩy về Elasticsearch, xem qua Kibana                                                 |
| **Consistency**     | Chấp nhận eventual consistency giữa transaction-service và account-service qua Kafka; có cơ chế bù trừ khi thất bại (compensating action) |
| **Performance**     | API tra cứu số dư/lịch sử phản hồi < 300ms ở mức tải demo (JMeter test)                                                                   |


---



## 5. Bảo Mật & Xác Thực (Security & Authentication)



### 5.1. Luồng OAuth2 (Authorization Code / Password Grant cho demo)

1. Client đăng nhập qua Keycloak → nhận `access_token` (JWT) và `refresh_token`.
2. Client gọi API kèm header `Authorization: Bearer <access_token>`.
3. Mỗi service dùng **Spring Security OAuth2 Resource Server** để verify chữ ký JWT qua JWKS endpoint của Keycloak (không cần gọi Keycloak mỗi request).
4. Spring Security ánh xạ `realm_access.roles` trong token thành `GrantedAuthority` (custom `JwtAuthenticationConverter`).



### 5.2. Phân quyền (Authorization)

- Roles trong Keycloak Realm: `CUSTOMER`, `TELLER`, `ADMIN`.
- Áp dụng `@PreAuthorize("hasRole('ADMIN')")` hoặc method security cho các endpoint nhạy cảm.
- Rule nghiệp vụ bổ sung: `CUSTOMER` chỉ được thao tác trên `accountId` gắn với `sub` (user id) của chính họ trong token — kiểm tra thủ công trong service layer, không chỉ dựa vào role.



### 5.3. Cấu hình Keycloak cần thiết lập

- Tạo Realm riêng (vd: `banking-demo`).
- Tạo Client `banking-api` (confidential, dùng client-credentials/secret lưu trong Vault).
- Tạo Roles: `CUSTOMER`, `TELLER`, `ADMIN`.
- Tạo vài user demo gắn role tương ứng để test các luồng phân quyền.

---



## 6. Khả Năng Chịu Lỗi (Resilience4j)


| Kỹ thuật                 | Áp dụng ở đâu                                             | Cấu hình gợi ý                                                                                            |
| ------------------------ | --------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| **Circuit Breaker**      | transaction-service gọi account-service                   | Mở circuit khi tỷ lệ lỗi > 50% trong 10 request gần nhất; fallback trả lỗi `ACCOUNT_SERVICE_UNAVAILABLE`  |
| **Retry**                | transaction-service gọi account-service                   | Retry tối đa 3 lần, backoff 200ms (chỉ retry với lỗi tạm thời như timeout, không retry lỗi nghiệp vụ 4xx) |
| **Rate Limiter**         | API public của transaction-service (khởi tạo giao dịch)   | Giới hạn N request/giây/user để chống spam giao dịch                                                      |
| **Bulkhead** *(mở rộng)* | Giới hạn số thread pool gọi song song đến account-service | Tránh 1 service chậm kéo sập toàn bộ thread pool                                                          |


> Ghi log riêng khi Circuit Breaker chuyển trạng thái (CLOSED → OPEN → HALF_OPEN) để chứng minh cơ chế hoạt động thật khi demo/phỏng vấn.

---



## 7. Quản Lý Secret (HashiCorp Vault)

- Lưu trữ: DB username/password, Keycloak client secret, Kafka credentials (nếu có SASL).
- Mỗi service đọc secret khi khởi động qua **Spring Cloud Vault** (`bootstrap.yml` hoặc `spring.config.import=vault://`).
- Dùng **AppRole authentication** cho service-to-Vault authentication (phù hợp môi trường non-human/service).
- (Mở rộng) Bật **dynamic secrets** cho PostgreSQL: Vault tự sinh username/password có thời hạn (TTL) thay vì credential tĩnh — đây là điểm rất ấn tượng khi phỏng vấn vì thể hiện hiểu biết Vault ở mức nâng cao.

---



## 8. Giao Tiếp Bất Đồng Bộ (Kafka)



### 8.1. Danh sách Topic


| Topic                     | Producer            | Consumer             | Payload chính                                         |
| ------------------------- | ------------------- | -------------------- | ----------------------------------------------------- |
| `transaction.created`     | transaction-service | account-service      | `transactionId`, `fromAccount`, `toAccount`, `amount` |
| `account.balance.updated` | account-service     | transaction-service  | `accountId`, `newBalance`, `transactionId`            |
| `transaction.completed`   | transaction-service | notification-service | `transactionId`, `status`, `customerId`               |
| `transaction.failed`      | transaction-service | notification-service | `transactionId`, `errorCode`, `customerId`            |




### 8.2. Yêu cầu kỹ thuật

- Dùng **Avro hoặc JSON Schema** cho message (khuyến nghị Avro + Schema Registry nếu muốn nâng độ khó, JSON đủ cho bản demo cơ bản).
- Consumer group riêng cho từng service.
- Xử lý **idempotent consumer**: lưu `transactionId` đã xử lý để tránh xử lý trùng khi Kafka redeliver.
- Cấu hình **Dead Letter Topic (DLT)** cho message xử lý lỗi liên tục để không chặn consumer.

---



## 9. Log Tập Trung & Quan Sát Hệ Thống (Elasticsearch / EFK)

- Mỗi service log ra **JSON structured log** (dùng Logback + `logstash-logback-encoder`), bao gồm: `timestamp`, `service`, `traceId`, `level`, `message`.
- Sinh `traceId`/`correlationId` ở request đầu vào (filter/interceptor), truyền qua header giữa các service (và đính vào Kafka message header) để trace xuyên suốt một giao dịch qua nhiều service.
- **Filebeat** (hoặc Logstash) thu thập log container → đẩy vào **Elasticsearch** → trực quan hoá bằng **Kibana** (dashboard: tỷ lệ lỗi theo service, latency, số giao dịch/phút).
- (Mở rộng, điểm cộng theo JD) Tích hợp **OpenTelemetry + Jaeger** để có distributed tracing đầy đủ, không chỉ log.

---



## 10. Xử Lý Lỗi Chuẩn Hoá (Exception Handling)



### 10.1. Response lỗi chuẩn (áp dụng cho toàn bộ service)

```json
{
  "timestamp": "2026-07-21T10:15:30Z",
  "traceId": "8f1c2e3a-...",
  "errorCode": "TXN_INSUFFICIENT_BALANCE",
  "message": "Số dư tài khoản không đủ để thực hiện giao dịch",
  "path": "/api/v1/transactions"
}
```



### 10.2. Bảng mã lỗi mẫu


| Error Code                    | HTTP Status | Ý nghĩa                                                          |
| ----------------------------- | ----------- | ---------------------------------------------------------------- |
| `ACC_NOT_FOUND`               | 404         | Không tìm thấy tài khoản                                         |
| `ACC_INACTIVE`                | 409         | Tài khoản đã bị khoá/ngừng hoạt động                             |
| `TXN_INSUFFICIENT_BALANCE`    | 422         | Số dư không đủ                                                   |
| `TXN_INVALID_AMOUNT`          | 400         | Số tiền không hợp lệ (âm, bằng 0)                                |
| `AUTH_INVALID_TOKEN`          | 401         | Token không hợp lệ hoặc hết hạn                                  |
| `AUTH_FORBIDDEN_RESOURCE`     | 403         | Không có quyền thao tác trên tài khoản này                       |
| `ACCOUNT_SERVICE_UNAVAILABLE` | 503         | Circuit breaker đang mở, account-service tạm thời không khả dụng |
| `TXN_DUPLICATE_REQUEST`       | 409         | Giao dịch trùng (idempotency check)                              |




### 10.3. Yêu cầu kỹ thuật

- Định nghĩa `BusinessException` (custom exception có `errorCode`) làm base class, các exception cụ thể kế thừa từ đây.
- Dùng `@RestControllerAdvice` + `@ExceptionHandler` để tập trung xử lý, tránh try-catch rải rác.
- Không để lộ stack trace hoặc thông tin nhạy cảm ra response cho client.

---



## 11. Thiết Kế Dữ Liệu (tóm tắt)

**account-service (PostgreSQL/Oracle)**

- `accounts (id, owner_user_id, account_number, balance, status, created_at)`

**transaction-service**

- `transactions (id, from_account, to_account, amount, status, error_code, created_at, updated_at)`

**notification-service**

- `notifications (id, transaction_id, customer_id, channel, status, sent_at)` — bảng log để chứng minh đã "gửi" thông báo (giả lập, không cần tích hợp SMTP/SMS thật).

---



## 12. Đóng Gói & Triển Khai



### 12.1. Docker Compose (môi trường local/dev)

Các service cần khai báo trong `docker-compose.yml`:
`postgres` (hoặc `oracle-xe`), `keycloak`, `kafka` + `zookeeper` (hoặc Kafka KRaft mode), `vault`, `elasticsearch`, `kibana`, `filebeat`/`logstash`, `account-service`, `transaction-service`, `notification-service`.

- Dùng **multi-stage Dockerfile** cho từng service (build bằng Maven/Gradle ở stage 1, copy jar sang base image nhẹ như `eclipse-temurin:21-jre-alpine` ở stage 2).
- Dùng `.env` + biến môi trường để không hard-code cấu hình.
- Healthcheck cho từng service trong `docker-compose.yml` (dùng Spring Boot Actuator `/actuator/health`).



### 12.2. Kubernetes (triển khai "production-like")

Với mỗi service cần có:

- `Deployment` (replicas ≥ 2, resource requests/limits, liveness & readiness probe trỏ vào Actuator).
- `Service` (ClusterIP nội bộ).
- `ConfigMap` cho cấu hình không nhạy cảm.
- `Secret` (tích hợp với Vault qua Vault Agent Injector hoặc External Secrets Operator — điểm cộng lớn khi demo).
- `Ingress` cho entrypoint public (nếu expose ra ngoài cụm).
- `HorizontalPodAutoscaler` cho transaction-service.
- Namespace riêng: `banking-demo`.

(Mở rộng, nếu muốn nâng độ khó): đóng gói bằng **Helm chart** thay vì raw YAML, và dùng **ArgoCD** để triển khai theo mô hình GitOps.

### 12.3. CI/CD Pipeline (GitHub Actions / GitLab CI)

Gợi ý các stage:

1. **Build & Unit Test** — build từng service, chạy unit test (JUnit/Mockito).
2. **Integration Test** — dùng **Testcontainers** để test thật với PostgreSQL/Kafka container (không mock hoàn toàn) → đây là điểm rất thực tế, nên làm.
3. **Static Analysis** *(tuỳ chọn)* — SonarQube/SonarCloud, kiểm tra code smell/security hotspot.
4. **Build & Push Docker Image** — build image, tag theo commit SHA, push lên registry (Docker Hub/GitHub Container Registry).
5. **Deploy** — apply manifest/Helm chart lên Kubernetes cluster (local: kind/minikube; hoặc free-tier cloud K8s).
6. Tách pipeline theo nhánh: `dev` → auto-deploy môi trường dev; `main`/tag → deploy môi trường staging (manual approval).

---



## 13. Kiểm Thử (Testing Strategy)


| Loại test                 | Công cụ                            | Phạm vi                                                            |
| ------------------------- | ---------------------------------- | ------------------------------------------------------------------ |
| Unit test                 | JUnit 5, Mockito                   | Business logic, exception handling                                 |
| Integration test          | Testcontainers (PostgreSQL, Kafka) | Repository, Kafka producer/consumer thật                           |
| API test                  | Postman/Newman                     | End-to-end luồng nghiệp vụ (đăng nhập → chuyển tiền → xem lịch sử) |
| Performance test          | JMeter                             | Đo throughput/latency API chuyển tiền dưới tải                     |
| Security test *(mở rộng)* | OWASP ZAP baseline scan            | Kiểm tra lỗ hổng cơ bản (OWASP Top 10) trên API                    |


---



## 14. Cấu Trúc Repository Gợi Ý

```
banking-microservices-demo/
├── account-service/
├── transaction-service/
├── notification-service/
├── infra/
│   ├── docker-compose.yml
│   ├── keycloak/          # realm export JSON để import nhanh
│   ├── k8s/                # manifest hoặc helm chart
│   └── vault/              # policy, config mẫu
├── docs/
│   ├── architecture-diagram.png
│   └── postman-collection.json
├── .github/workflows/      # CI/CD pipeline
└── README.md
```

---



## 15. Lộ Trình Triển Khai Gợi Ý (để "vận hành thực tế", không chỉ code)


| Giai đoạn    | Nội dung                                                                                                                | Mục tiêu chứng minh                                              |
| ------------ | ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| **Tuần 1**   | account-service + transaction-service (REST thuần, chưa có Kafka/Security) chạy được với PostgreSQL, Docker Compose     | Nền tảng Spring Boot + DB vững                                   |
| **Tuần 2**   | Thêm Kafka giữa 2 service, notification-service consume event                                                           | Hiểu event-driven thật, không chỉ lý thuyết                      |
| **Tuần 3**   | Tích hợp Keycloak (OAuth2 Resource Server + role-based authorization)                                                   | Chứng minh bảo mật thực chiến                                    |
| **Tuần 4**   | Resilience4j (Circuit Breaker/Retry/Rate Limiter) + custom exception handling toàn hệ thống                             | Chứng minh tư duy fault-tolerance                                |
| **Tuần 5**   | Vault (secret động cho DB) + EFK stack cho log tập trung                                                                | Chứng minh observability & secret management                     |
| **Tuần 6**   | Viết Kubernetes manifest, deploy lên minikube/kind; CI/CD pipeline build-test-deploy                                    | Chứng minh vận hành DevOps thực tế                               |
| **Tuần 7-8** | Testcontainers cho integration test, JMeter test hiệu năng, viết README + kiến trúc diagram, quay demo ngắn (video/gif) | Đóng gói thành portfolio hoàn chỉnh, sẵn sàng show khi phỏng vấn |


> **Gợi ý khi phỏng vấn:** chuẩn bị sẵn 1 bản kiến trúc diagram + 2-3 phút demo video (chuyển tiền → xem log trên Kibana → chủ động tắt account-service để show Circuit Breaker chuyển OPEN → gọi lại API vẫn nhận lỗi rõ ràng thay vì crash). Đây chính là bằng chứng "vận hành thực tế" thuyết phục nhất, mạnh hơn nhiều so với chỉ liệt kê công nghệ trong CV.

---



## 16. Định Nghĩa Hoàn Thành (Definition of Done)

Dự án được coi là hoàn chỉnh để đưa vào CV/portfolio khi:

- [ ] `docker-compose up` chạy được toàn bộ hệ thống ở local chỉ với 1 lệnh.
- [ ] Luồng chuyển tiền end-to-end hoạt động đúng, có xử lý cả case lỗi (số dư không đủ, tài khoản không tồn tại).
- [ ] Đăng nhập qua Keycloak, phân quyền theo role hoạt động đúng (test với ít nhất 2 role khác nhau).
- [ ] Circuit Breaker chứng minh được bằng cách chủ động tắt 1 service.
- [ ] Log hiển thị được trên Kibana, tìm được theo `traceId`.
- [ ] Secret không xuất hiện dạng plain-text trong source code hay `docker-compose.yml`.
- [ ] Có ít nhất 1 pipeline CI/CD chạy thành công (build → test → build image → deploy).
- [ ] README có kiến trúc diagram, hướng dẫn chạy, và giải thích ngắn từng kỹ thuật đã áp dụng.