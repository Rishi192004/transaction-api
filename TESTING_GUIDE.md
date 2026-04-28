# Transaction API - Comprehensive Testing Guide

## 1️⃣ UNIT TESTS (Fast - Run Locally)

### Rate Limiter Tests
```bash
# Test token bucket algorithm
mvn test -Dtest=AccountRateLimitStoreTest

# Verify per-account limits
# Verify bucket reset
# Verify custom limits
```

### Cache Service Tests
```bash
# Test Redis operations
mvn test -Dtest=CacheServiceTest

# Verify cache hit/miss
# Verify TTL expiration
# Verify cache invalidation
```

### Event Publisher Tests
```bash
# Test Kafka event publishing
mvn test -Dtest=TransactionEventPublisherTest

# Verify event payload
# Verify message headers
# Verify topic routing
```

---

## 2️⃣ INTEGRATION TESTS (Medium - Database + External Services)

### Run All Integration Tests
```bash
mvn verify
```

### Specific Integration Tests
```bash
# Full transaction lifecycle
mvn test -Dtest=TransactionControllerIntegrationTest#testCreateTransactionSuccess

# Idempotency verification
mvn test -Dtest=TransactionControllerIntegrationTest#testCreateTransactionDuplicate

# Rate limiting behavior
mvn test -Dtest=TransactionControllerIntegrationTest#testRateLimitingExceeded

# Cache validation
mvn test -Dtest=TransactionControllerIntegrationTest#testGetTransactionsByAccountCached
```

---

## 3️⃣ MANUAL TESTING (Postman / curl)

### Setup Prerequisites
```bash
# Start Redis
redis-server

# Start Kafka (or use Docker)
docker-compose up -d kafka zookeeper

# Start application
mvn spring-boot:run
```

### 📝 Test Scenarios

#### **A. Create Transaction (Idempotency Test)**
```bash
# Test 1: Create new transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "accountId": "ACC-001",
    "amount": 1000.00,
    "type": "DEPOSIT",
    "description": "Monthly salary",
    "idempotencyKey": "DEPOSIT-20260428-001"
  }'

# Response: 201 Created
# {
#   "id": 1,
#   "accountId": "ACC-001",
#   "amount": 1000.00,
#   "status": "COMPLETED",
#   ...
# }

# Test 2: Repeat with same idempotencyKey
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "accountId": "ACC-001",
    "amount": 1000.00,
    "type": "DEPOSIT",
    "description": "Monthly salary",
    "idempotencyKey": "DEPOSIT-20260428-001"
  }'

# Response: 409 Conflict
# {
#   "status": 409,
#   "message": "Duplicate transaction..."
# }
```

#### **B. Rate Limiting Test**
```bash
# Rapidly send 101 requests in < 1 minute

for i in {1..101}; do
  curl -X POST http://localhost:8080/api/v1/transactions \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -d "{\"accountId\": \"ACC-001\", \"amount\": 10, \"type\": \"DEPOSIT\", \"description\": \"Test $i\", \"idempotencyKey\": \"REQ-$i\"}" \
    -w "Status: %{http_code}\n" \
    -o /dev/null -s
done

# First 100: 201 Created
# 101st: 429 Too Many Requests
```

#### **C. Caching Test**
```bash
# First request (cache miss)
time curl -X GET "http://localhost:8080/api/v1/transactions/account/ACC-001?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response time: ~50-100ms (database query)

# Second request (cache hit)
time curl -X GET "http://localhost:8080/api/v1/transactions/account/ACC-001?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response time: ~5-10ms (Redis cache)
```

#### **D. Kafka Event Publishing Test**
```bash
# Monitor Kafka topic for events
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic transaction-events \
  --from-beginning

# Create transaction and observe event in console
# Should see: {"transactionId": 1, "accountId": "ACC-001", "eventType": "CREATED", ...}
```

#### **E. Settlement Event Processing**
```bash
# Monitor settlement topic
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic settlement-events \
  --from-beginning

# Create transaction with large amount
# Observe: settlement event published and processed asynchronously
# Check logs for: "Settlement completed successfully for transaction"
```

---

## 4️⃣ LOAD TESTING (Apache JMeter / Gatling)

### JMeter Test Plan
```
Thread Group:
  - 50 threads
  - Ramp-up: 30 seconds
  - Duration: 5 minutes

HTTP Requests:
  1. Create Transaction (POST)
  2. Get Transaction (GET)
  3. List Transactions (GET)
  4. Reverse Transaction (PATCH - 10% of threads)

Metrics to Observe:
  - Throughput: transactions/sec
  - Response time: avg, min, max, 95th percentile
  - Error rate
  - Rate limiter rejection rate
```

### Run Load Test
```bash
# Using ab (Apache Bench)
ab -n 1000 -c 50 \
  -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/v1/transactions/account/ACC-001

# Using wrk
wrk -t4 -c100 -d30s \
  -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/v1/transactions/account/ACC-001
```

---

## 5️⃣ CHAOS TESTING (Resilience)

### Redis Failure Scenario
```bash
# Stop Redis
redis-cli SHUTDOWN

# Attempt transaction creation
# Expected: Succeeds without Redis (graceful degradation)
# Cache just won't work but API still functions
```

### Kafka Failure Scenario
```bash
# Kill Kafka broker
docker-compose stop kafka

# Create transaction
# Expected: Transaction created but events not published
# Check logs for Kafka connection errors
```

### Database Failure Scenario
```bash
# Connection pool exhaustion test
# Send many concurrent requests without releasing connections
# Observe: Queue buildup, connection timeouts
```

---

## 6️⃣ SECURITY TESTING

### JWT Authentication
```bash
# Missing token
curl -X GET http://localhost:8080/api/v1/transactions/1
# Response: 401 Unauthorized

# Invalid token
curl -X GET http://localhost:8080/api/v1/transactions/1 \
  -H "Authorization: Bearer INVALID_TOKEN"
# Response: 401 Unauthorized

# Expired token
# Response: 401 Unauthorized
```

### Authorization (RBAC)
```bash
# User trying to reverse transaction (admin only)
curl -X PATCH http://localhost:8080/api/v1/transactions/1/reverse \
  -H "Authorization: Bearer USER_TOKEN"
# Response: 403 Forbidden

# Admin reversing transaction
curl -X PATCH http://localhost:8080/api/v1/transactions/1/reverse \
  -H "Authorization: Bearer ADMIN_TOKEN"
# Response: 200 OK
```

### SQL Injection Test
```bash
# Try injection in query parameter
curl -X GET "http://localhost:8080/api/v1/transactions/account/ACC' OR '1'='1" \
  -H "Authorization: Bearer TOKEN"
# Response: Parameter validated, no injection vulnerability
```

---

## 7️⃣ MONITORING & OBSERVABILITY

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health
# Response: {"status":"UP"}

# Detailed health
curl http://localhost:8080/actuator/health/readiness
# Shows: database status, kafka status, redis status
```

### Metrics
```bash
# Get Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Key metrics to track:
# - http_requests_total (by endpoint)
# - http_request_duration_seconds
# - jvm_memory_used_bytes
# - kafka_producer_record_send_total
```

### Logging
```bash
# Enable DEBUG logging
export LOGGING_LEVEL_COM_RISHI_TRANSACTIONAPI=DEBUG
mvn spring-boot:run

# Watch transaction flow in console
# Will see: Rate limit check → DB save → Event publish → Cache update
```

---

## 8️⃣ PERFORMANCE TESTING

### Metrics to Measure
| Metric | Target | Tool |
|--------|--------|------|
| **Throughput** | >1000 txn/sec | JMeter, Wrk |
| **Latency (p95)** | <100ms | JMeter |
| **Latency (cached)** | <20ms | Postman |
| **Cache Hit Rate** | >80% | Redis logs |
| **Error Rate** | <0.1% | Actuator |

### Database Performance
```sql
-- Monitor slow queries
SELECT * FROM information_schema.processlist WHERE time > 5;

-- Check indexes
SHOW INDEX FROM transactions;

-- Connection pool status
SELECT count(*) FROM pg_stat_activity;
```

---

## 9️⃣ REGRESSION TESTING (After Changes)

```bash
# Run full test suite before deployment
mvn clean test verify

# Check coverage (aim for >80%)
mvn jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

---

## 🔟 TESTING CHECKLIST

- [ ] Unit tests pass (mvn test)
- [ ] Integration tests pass (mvn verify)
- [ ] Manual API tests with Postman
- [ ] Rate limiter tested under load
- [ ] Cache invalidation verified
- [ ] Kafka events published & consumed
- [ ] Authentication & authorization working
- [ ] Error handling & exception messages clear
- [ ] Idempotency key prevents duplicates
- [ ] Performance targets met
- [ ] Security vulnerabilities checked
- [ ] Code coverage >80%
- [ ] Load test completed (50+ concurrent)
- [ ] Chaos test scenarios passed
- [ ] Monitoring alerts configured

---

## 📊 Generate Test Report
```bash
# Run all tests with report
mvn clean test -Dorg.slf4j.simpleLogger.defaultLogLevel=info

# Generate HTML report
mvn surefire-report:report

# View report
open target/site/surefire-report.html
```

---

## 🚀 CI/CD Integration

### GitHub Actions Workflow
```yaml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      redis: # Start Redis container
      kafka: # Start Kafka container
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - run: mvn clean test verify
      - run: mvn jacoco:report
      - uses: codecov/codecov-action@v2
```

---

## 📚 Test Data Setup

```sql
-- Insert test users
INSERT INTO users VALUES ('user1', 'password', 'ROLE_USER');
INSERT INTO users VALUES ('admin1', 'password', 'ROLE_ADMIN');

-- Insert test transactions
INSERT INTO transactions (account_id, amount, type, status) 
VALUES ('ACC-001', 1000, 'DEPOSIT', 'COMPLETED');
```
