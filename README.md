# Transaction Management API

A **production-grade RESTful microservice** for managing financial transactions with enterprise-level features including **rate limiting**, **Redis caching**, **Kafka event-driven architecture**, and comprehensive security.

Built with **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, and tested with **JUnit 5**.

## 🎯 Core Features

✅ **Idempotent Transactions** — Prevent duplicates with idempotency keys  
✅ **Per-Account Rate Limiting** — Token bucket algorithm (100 req/min default)  
✅ **Redis Caching Layer** — Cache balances & recent transactions (5-30 min TTL)  
✅ **Kafka Event Publishing** — Async settlement processing  
✅ **JWT Authentication** — Stateless token-based security  
✅ **Role-Based Access Control** — USER and ADMIN roles  
✅ **Paginated Queries** — Account history with date-range filtering  
✅ **OpenAPI/Swagger** — Auto-generated API documentation  
✅ **Global Exception Handling** — Consistent error responses  
✅ **Spring Actuator** — Health checks & metrics  

## Tech Stack

| Layer | Technology |
|---|---|
| **Framework** | Spring Boot 3.2, Spring MVC |
| **Security** | Spring Security, JWT, RBAC |
| **Persistence** | Spring Data JPA, H2 (dev) / PostgreSQL (prod) |
| **Caching** | Spring Data Redis, Lettuce |
| **Messaging** | Spring Kafka |
| **Rate Limiting** | Bucket4j (Token Bucket Algorithm) |
| **Build** | Maven |
| **Testing** | JUnit 5, Mockito, MockMvc |
| **Docs** | OpenAPI 3.0, Springdoc |
| **Monitoring** | Spring Actuator, Micrometer |

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Redis (optional, for caching)
- Kafka (optional, for async settlement)

### Local Setup
```bash
# Clone repository
git clone https://github.com/Rishi192004/transaction-api.git
cd transaction-api

# Build
mvn clean install

# Run
mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`

### With Redis & Kafka (Optional)
```bash
# Start Redis
redis-server

# Start Kafka (Docker)
docker-compose up kafka zookeeper

# Then run API
mvn spring-boot:run
```

## 📋 API Endpoints

### Create Transaction
```bash
POST /api/v1/transactions
Content-Type: application/json
Authorization: Bearer JWT_TOKEN

{
  "accountId": "ACC-001",
  "amount": 1000.00,
  "type": "DEPOSIT",
  "description": "Monthly salary",
  "idempotencyKey": "unique-key-001"
}

Response: 201 Created
{
  "id": 1,
  "accountId": "ACC-001",
  "amount": 1000.00,
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "createdAt": "2026-04-28T10:30:45"
}
```

### Get Transaction
```bash
GET /api/v1/transactions/{id}
Authorization: Bearer JWT_TOKEN

Response: 200 OK
```

### List Transactions (Cached)
```bash
GET /api/v1/transactions/account/{accountId}?page=0&size=20
Authorization: Bearer JWT_TOKEN

Response: 200 OK (with pagination)
```

### Reverse Transaction (Admin Only)
```bash
PATCH /api/v1/transactions/{id}/reverse
Authorization: Bearer ADMIN_JWT_TOKEN

Response: 200 OK
```

## ⚙️ Configuration

### Rate Limiting
```properties
# Default: 100 requests/minute per account
# Custom limits via admin API or code
```

### Caching (Redis)
```properties
spring.redis.host=localhost
spring.redis.port=6379
app.cache.redis.enabled=true

# TTLs:
# - Account balance: 5 minutes
# - Recent transactions: 10 minutes  
# - Account info: 30 minutes
```

### Kafka (Async Settlement)
```properties
spring.kafka.bootstrap-servers=localhost:9092
app.kafka.enabled=true

# Topics:
# - transaction-events: CREATE/UPDATE/SETTLED events
# - settlement-events: Async settlement processing
```

## 🧪 Testing

### Run Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean test jacoco:report
```

### Manual Testing
See `TESTING_GUIDE.md` for comprehensive testing strategies including:
- ✅ Unit tests
- ✅ Integration tests
- ✅ Load testing with JMeter
- ✅ Chaos testing
- ✅ Security testing
- ✅ Performance benchmarking

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| **CODEBASE_OVERVIEW.md** | Complete overview of what the system does |
| **TESTING_GUIDE.md** | Comprehensive testing strategies (10+ methods) |
| **ADVANCED_FEATURES_ROADMAP.md** | 10+ advanced features with effort/ROI analysis |

## 🚀 Advanced Features

Current implementation includes:
- ✅ Custom per-account rate limiter
- ✅ Redis caching layer  
- ✅ Kafka event publishing

Roadmap features (easy to add):
- Prometheus metrics + Grafana
- Distributed tracing (Sleuth/Zipkin)
- Circuit breaker pattern (Resilience4j)
- WebSocket real-time notifications
- Audit logging for compliance
- Transaction reconciliation engine
- Multi-tenancy support
- And 10+ more...

See `ADVANCED_FEATURES_ROADMAP.md` for complete details.

## 🏗️ Architecture

## Running Tests

```bash
mvn test
```

## Production Swap Notes

- Replace H2 datasource with PostgreSQL/Oracle in `application.properties`
- Replace in-memory `UserDetailsService` with DB-backed implementation
- Add Kafka/SQS publisher in `TransactionService` for async settlement
- Enable structured JSON logging for ELK ingestion
