# fintech-gateway

An intelligent API security middleware built with Spring Boot that intercepts every incoming request, evaluates its risk in real time, and makes an adaptive allow/throttle/block decision before forwarding to any backend service.

---

## Table of Contents

1. [What This Service Does](#what-this-service-does)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [How It Works](#how-it-works)
5. [Security Pipeline](#security-pipeline)
6. [Risk Scoring](#risk-scoring)
7. [Rate Limiting](#rate-limiting)
8. [Attack Detection](#attack-detection)
9. [Idempotency](#idempotency)
10. [Trust Tier System](#trust-tier-system)
11. [Audit Logging](#audit-logging)
12. [Configuration](#configuration)
13. [Running Locally](#running-locally)
14. [Running with Docker](#running-with-docker)
15. [API Endpoints](#api-endpoints)
16. [Testing](#testing)
17. [Known Limitations](#known-limitations)

---

## What This Service Does

The gateway sits between any client and any backend. Every request passes through a multi-layer security pipeline before the backend ever sees it.

```
Client Request
      ↓
[ fintech-gateway :8081 ]
      ↓ (if ALLOW)
[ fintech-backend :8080 ]
```

It solves five real problems:

- **Stops attacks** — SQL injection, XSS, and path traversal are caught at the gateway, not inside business logic
- **Prevents abuse** — rate limiting with progressive throttling stops bots and scrapers
- **Prevents duplicate payments** — idempotency keys ensure a payment is processed exactly once even under network failures
- **Tracks reputation** — trust tiers give every IP a persistent risk profile that survives gateway restarts
- **Creates audit trail** — every security decision is logged to console, file, and MySQL for compliance

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.5 | Framework |
| Spring Web MVC | 7.0.6 | HTTP layer |
| Spring Data JPA | — | MySQL persistence |
| Spring Data Redis | — | State management |
| Hibernate | 7.2.7 | ORM |
| Lettuce | 6.8.2 | Redis client |
| MySQL Connector | 9.6.0 | Database driver |
| SpringDoc OpenAPI | 3.0.2 | Swagger UI |
| Logback | 1.5.32 | Logging |
| JUnit 5 | 6.0.3 | Testing |
| Mockito | 5.20.0 | Mocking |
| Maven | 3.x | Build |

---

## Project Structure

```
fintech-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/fintech/fintech_gateway/
│   │   │   ├── FintechGatewayApplication.java      ← Entry point, @EnableAsync
│   │   │   │
│   │   │   ├── attack/
│   │   │   │   └── AttackDetectionService.java     ← Regex-based threat detection
│   │   │   │
│   │   │   ├── audit/
│   │   │   │   └── AuditLogger.java                ← Structured event logging
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── GatewayConfig.java              ← RestClient bean, CORS
│   │   │   │   ├── RedisConfig.java                ← RedisTemplate<String,String>
│   │   │   │   └── SecurityHeadersFilter.java      ← Adds security headers
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── GatewayController.java          ← /** wildcard proxy handler
│   │   │   │   ├── HealthController.java           ← /health endpoint
│   │   │   │   └── PingController.java             ← /ping endpoint
│   │   │   │
│   │   │   ├── feedback/
│   │   │   │   ├── FeedbackController.java         ← POST /feedback/false-positive
│   │   │   │   └── FeedbackService.java            ← Unblock IP, reset trust
│   │   │   │
│   │   │   ├── filter/
│   │   │   │   ├── RequestLoggingFilter.java       ← Main security pipeline
│   │   │   │   ├── CachedBodyRequestWrapper.java   ← Re-readable request body
│   │   │   │   └── CachedResponseWrapper.java      ← Capturable response body
│   │   │   │
│   │   │   ├── idempotency/
│   │   │   │   └── IdempotencyService.java         ← Three-state Redis tracking
│   │   │   │
│   │   │   ├── persistence/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── AuditLogEntity.java         ← audit_logs table mapping
│   │   │   │   │   └── TrustHistoryEntity.java     ← trust_history table mapping
│   │   │   │   ├── repository/
│   │   │   │   │   ├── AuditLogRepository.java
│   │   │   │   │   └── TrustHistoryRepository.java
│   │   │   │   └── PersistenceService.java         ← @Async DB writes
│   │   │   │
│   │   │   ├── proxy/
│   │   │   │   └── ProxyService.java               ← HTTP forwarding to backend
│   │   │   │
│   │   │   ├── risk/
│   │   │   │   ├── RiskScoringEngine.java          ← 4-factor score (0-100)
│   │   │   │   └── RiskDecisionEngine.java         ← Score → decision
│   │   │   │
│   │   │   └── trust/
│   │   │       └── TrustTierService.java           ← IP reputation management
│   │   │
│   │   └── resources/
│   │       ├── application.properties              ← Base config with ${VAR:default}
│   │       ├── application-dev.properties          ← Dev overrides
│   │       ├── application-prod.properties         ← Prod overrides
│   │       └── logback-spring.xml                  ← Rolling file logging
│   │
│   └── test/
│       └── java/com/fintech/fintech_gateway/
│           ├── attack/
│           │   └── AttackDetectionServiceTest.java  ← 5 tests
│           ├── risk/
│           │   └── RiskDecisionEngineTest.java      ← 7 tests
│           └── FintechGatewayApplicationTests.java  ← Placeholder
│
└── pom.xml
```

---

## How It Works

Every request that arrives at port 8081 goes through the filter chain in this exact order:

```
1. SecurityHeadersFilter      ← Runs first, adds security headers to ALL responses
        ↓
2. RequestLoggingFilter       ← Main pipeline
        ↓
   ┌── Skip check ────────────── /favicon.ico, /swagger-ui, /v3/api-docs,
   │                              /feedback, /health, /ping, /actuator → bypass everything
        ↓
   ├── Blacklist check ──────── Is this IP permanently banned? → 403 immediately
        ↓
   ├── Risk Scoring ──────────── Calculate score 0-100 from 4 signals
        ↓
   ├── Decision Engine ───────── Score → ALLOW / THROTTLE / BLOCK
        ↓
   ├── Throttle handling ─────── If THROTTLE: progressive delay + strike tracking
   │                              If BLOCK: 429 response + recordBlock()
        ↓
   ├── Attack Detection ──────── Inspect body for SQLi, XSS, path traversal
        ↓
   ├── Idempotency Check ─────── POST only: check/set idempotency key state
        ↓
   └── ProxyService ──────────── Forward to backend, capture response
        ↓
   AuditLogger (always)          Log to console + file + MySQL (async)
   TrustTierService (on ALLOW)   Record good behaviour
```

---

## Security Pipeline

### SecurityHeadersFilter

Runs at `@Order(1)` — before everything else. Adds these headers to every single response:

```
X-Frame-Options: DENY                              → prevents clickjacking
X-Content-Type-Options: nosniff                    → prevents MIME sniffing
Strict-Transport-Security: max-age=31536000        → forces HTTPS (HSTS)
X-XSS-Protection: 1; mode=block                   → legacy XSS protection
Content-Security-Policy: default-src 'self'        → restricts resource loading
Referrer-Policy: no-referrer                       → prevents referrer leakage
```

### RequestLoggingFilter

The core security filter. Extends `OncePerRequestFilter` — Spring guarantees it runs exactly once per request regardless of forward/include chains.

All logic is wrapped in a top-level try-catch. If any component throws unexpectedly, the filter fails open — the request is forwarded to the backend rather than returning a 500 to the user.

---

## Risk Scoring

### Score Factors

| Factor | Signal | Points |
|---|---|---|
| Endpoint sensitivity | `/payments/*` | +30 |
| | `/account/balance` | +10 |
| | `/account/profile` or unknown | +5 |
| Request frequency | > 45 req/min from this IP | +40 |
| | > 30 req/min | +20 |
| | > 10 req/min | +10 |
| | ≤ 10 req/min | +0 |
| Time of request | 11PM – 5AM local time | +15 |
| | Daytime | +0 |
| Payload size | > 10,000 bytes | +20 |
| | > 5,000 bytes | +10 |
| | ≤ 5,000 bytes | +0 |
| Trust adjustment | HIGH tier | -10 |
| | MEDIUM tier | +0 |
| | LOW tier | +20 |
| | BLACKLISTED | +100 |

Final score is clamped: `Math.min(Math.max(score, 0), 100)`

### Decision Thresholds (configurable via env vars)

```
score >= RISK_BLOCK_THRESHOLD    (default 71) → BLOCK    → HTTP 429
score >= RISK_THROTTLE_THRESHOLD (default 31) → THROTTLE → delay + continue
score <  RISK_THROTTLE_THRESHOLD              → ALLOW    → forward immediately
```

---

## Rate Limiting

Uses Redis fixed-window with TTL-based reset (not a true sliding window — see Known Limitations).

```java
String key = "rate:" + ip;
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, Duration.ofMinutes(1));
}
```

- First request from an IP creates the key and sets 1-minute TTL
- Each subsequent request increments the counter
- After 1 minute, Redis auto-deletes the key — count resets
- All gateway instances share the same Redis key — accurate under horizontal scaling

### Throttle Strike Tracking (Threshold Surfing Prevention)

An attacker who stays just below the block threshold can probe indefinitely. To prevent this:

```
Throttle strike 1–2 → Thread.sleep(2000)   → 2 second delay
Throttle strike 3–4 → Thread.sleep(5000)   → 5 second delay
Throttle strike 5–6 → Thread.sleep(10000)  → 10 second delay
Throttle strike 7+  → Force BLOCK          → recordBlock()
```

Strike count expires after 10 minutes. Resets on any ALLOW decision.

---

## Attack Detection

Checks run in this order — order matters to prevent false positives:

### 1. Path Traversal (on URI — runs even for empty body)

```regex
.*\.\..*
(?i).*(etc/passwd|win/system32|windows/system32).*
```

### 2. XSS (on request body — checked BEFORE SQL injection)

```regex
(?i).*(<script|</script|javascript:|onerror=|onload=|<iframe|<img).*
(?i).*(alert\(|confirm\(|prompt\().*
```

XSS is checked before SQL because XSS payloads often contain single quotes which would trigger SQL injection patterns first, causing incorrect threat classification.

### 3. SQL Injection (on request body)

```regex
(?i).*(--|;|'|\bOR\b|\bAND\b|\bDROP\b|\bSELECT\b|\bINSERT\b|\bDELETE\b|\bUPDATE\b|\bUNION\b).*
(?i).*('\s*(or|and)\s*'?\d).*
(?i).*(\bEXEC\b|\bEXECUTE\b|\bxp_|\bsp_).*
```

**Why CachedBodyRequestWrapper exists:** HTTP request `InputStream` can only be read once. Attack detection reads the body to inspect it. Without the wrapper, the body would be consumed and unavailable when `ProxyService` tries to forward it.

The wrapper stores the body as a `byte[]` on first read and re-creates the stream on every `getInputStream()` call.

---

## Idempotency

### The Problem

A user clicks Pay. The payment processes successfully. The network times out before the response arrives. The app shows an error. The user clicks Pay again. Without idempotency, the payment is charged twice.

### Three-State Machine

```
NOT_EXISTS  → first request      → process normally, mark IN_PROGRESS
IN_PROGRESS → concurrent request → return 409 Conflict
COMPLETED   → duplicate request  → return cached response (same transaction ID)
```

```java
// Redis key format
"idempotency:" + idempotencyKey → "IN_PROGRESS" | JSON_response_string
// TTL: 24 hours
```

### How Response Is Captured

`HttpServletResponse` writes directly to the network output stream. By the time you want to cache the response, it's already sent.

`CachedResponseWrapper` overrides `getOutputStream()` to write to an internal `ByteArrayOutputStream` instead. After the filter chain completes:
1. Captured bytes are stored in Redis
2. Bytes are then written to the real network stream via `copyBodyToResponse()`

### Usage

Client must include `Idempotency-Key` header on POST requests:

```
POST /api/payments/transfer
Idempotency-Key: PAY-UUID-generated-by-client
```

---

## Trust Tier System

### Tiers

| Tier | Risk Adjustment | Description |
|---|---|---|
| HIGH | -10 | Verified, long history of clean requests |
| MEDIUM | 0 | Default for all new IPs |
| LOW | +20 | Has been blocked before |
| BLACKLISTED | +100 | Permanent ban |

### Progressive Penalty

```
Block 1 → LOW trust for 1 minute    (cooldown, then MEDIUM again)
Block 2 → LOW trust for 10 minutes
Block 3 → LOW trust for 1 hour
Block 4 → PERMANENT BLACKLIST        (never automatically restored)
```

### Redis Keys Per IP

| Key | TTL | Value |
|---|---|---|
| `trust:{ip}` | Varies | `HIGH` / `MEDIUM` / `LOW` |
| `blockcount:{ip}` | Never | Integer (permanent history) |
| `blacklist:{ip}` | Never | `"PERMANENT"` |
| `throttlecount:{ip}` | 10 minutes | Integer |
| `goodcount:{ip}` | 24 hours | Integer |
| `rate:{ip}` | 1 minute | Integer |

### Upgrading to HIGH Trust

An IP that makes 100 consecutive clean requests (ALLOW decisions) without being throttled or blocked is promoted to HIGH trust. The good behavior counter resets after 24 hours.

### Feedback API (False Positive Recovery)

When a legitimate user is wrongly blacklisted:

```
POST /feedback/false-positive
{"ip": "192.168.1.1", "reason": "legitimate user"}
```

This deletes `blacklist:{ip}`, `blockcount:{ip}`, and `trust:{ip}` from Redis, and saves a `BLACKLISTED → MEDIUM` record to `trust_history` in MySQL.

**Important:** This endpoint bypasses all security checks. In production it must be behind admin JWT authentication.

---

## Audit Logging

Every security event is logged in three places simultaneously:

### 1. Console (SLF4J)

```
AUDIT | timestamp=2026-04-19T10:15:30Z | ip=192.168.1.1 | method=POST |
uri=/api/payments/transfer | riskScore=30 | decision=ALLOW |
attackType=NONE | statusCode=200
```

### 2. File (Logback Rolling)

- Location: `logs/audit.log`
- Rolls daily: `logs/audit.2026-04-19.log`
- Retention: 30 days
- Configured in: `logback-spring.xml`

### 3. MySQL (Async)

Written via `@Async` PersistenceService — database writes never slow down the request pipeline. If a write fails, it logs to stderr but does not affect the user response.

```sql
INSERT INTO audit_logs (timestamp, ip, method, uri, risk_score, decision, attack_type, status_code)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
```

---

## Configuration

### application.properties (base — always loaded)

```properties
server.port=${SERVER_PORT:8081}
gateway.backend.url=${BACKEND_URL:http://localhost:8080}

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.timeout=2000
spring.data.redis.connect-timeout=2000

spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:FintechGateway}
spring.datasource.username=${MYSQL_USER:fintechuser}
spring.datasource.password=${MYSQL_PASSWORD:password}
spring.jpa.hibernate.ddl-auto=update

springdoc.swagger-ui.enabled=${SWAGGER_ENABLED:true}
springdoc.api-docs.enabled=${SWAGGER_ENABLED:true}

gateway.risk.block-threshold=${RISK_BLOCK_THRESHOLD:71}
gateway.risk.throttle-threshold=${RISK_THROTTLE_THRESHOLD:31}

spring.profiles.active=${SPRING_PROFILE:dev}
spring.web.resources.add-mappings=false
logging.config=classpath:logback-spring.xml
```

### application-dev.properties

```properties
springdoc.swagger-ui.enabled=true
spring.jpa.show-sql=true
logging.level.com.fintech=DEBUG
```

### application-prod.properties

```properties
springdoc.swagger-ui.enabled=false
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.fintech=WARN
```

### Environment Variable Reference

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8081` | Gateway listening port |
| `BACKEND_URL` | `http://localhost:8080` | Backend service URL |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `MYSQL_HOST` | `localhost` | MySQL hostname |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DB` | `FintechGateway` | Database name |
| `MYSQL_USER` | `fintechuser` | DB username |
| `MYSQL_PASSWORD` | `password` | DB password |
| `RISK_BLOCK_THRESHOLD` | `71` | Score to trigger BLOCK |
| `RISK_THROTTLE_THRESHOLD` | `31` | Score to trigger THROTTLE |
| `SPRING_PROFILE` | `dev` | Active Spring profile |
| `SWAGGER_ENABLED` | `true` | Enable/disable Swagger UI |

---

## Running Locally

### Prerequisites

- Java 21
- Maven 3.x
- Docker (for Redis and MySQL)

### Step 1 — Start Redis and MySQL

```bash
docker run -d --name fintech_redis -p 6379:6379 redis:7-alpine
docker run -d --name fintech_mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=FintechGateway \
  -e MYSQL_USER=fintechuser \
  -e MYSQL_PASSWORD=fintechpassword \
  mysql:8.0
```

### Step 2 — Start fintech-backend (required)

```bash
cd fintech-backend
./mvnw spring-boot:run
# Verify: http://localhost:8080/actuator/health
```

### Step 3 — Start fintech-gateway

```bash
cd fintech-gateway
./mvnw spring-boot:run
# Verify: http://localhost:8081/ping
```

### Step 4 — Test

```bash
# Health check
curl http://localhost:8081/health

# Normal request
curl http://localhost:8081/api/account/balance?accountId=ACC123

# SQL injection (should be blocked)
curl -X POST http://localhost:8081/api/payments/transfer \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: TEST-001" \
  -d '{"fromAccountId":"ACC123'\'' OR '\''1'\''='\''1","toAccountId":"ACC456","amount":5000,"currency":"INR","idempotencyKey":"TEST-001"}'
```

---

## Running with Docker

```bash
# From project root
cp .env.example .env
docker-compose up --build fintech-gateway
```

Gateway depends on MySQL and Redis being healthy first — docker-compose handles the startup order automatically.

---

## API Endpoints

### Proxy (all forwarded to backend)

| Method | Path | Description |
|---|---|---|
| GET | `/api/account/balance?accountId=X` | Get account balance |
| GET | `/api/account/profile?userId=X` | Get user profile |
| POST | `/api/payments/transfer` | Process payment |

### Gateway Internal

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | None | Service health status |
| GET | `/ping` | None | Uptime check for monitors |
| POST | `/feedback/false-positive` | None* | Unblock wrongly blocked IP |
| GET | `/swagger-ui/index.html` | None (dev only) | API documentation |

*Must be protected by admin auth in production

---

## Testing

### Run All Tests

```bash
./mvnw test
```

Expected: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`

### Test Classes

**RiskDecisionEngineTest** (7 tests)
- `score25_shouldAllow`
- `score50_shouldThrottle`
- `score75_shouldBlock`
- `scoreAtExactBlockThreshold_shouldBlock` (71)
- `scoreAtExactThrottleThreshold_shouldThrottle` (31)
- `score0_shouldAllow`
- `score100_shouldBlock`

**AttackDetectionServiceTest** (5 tests)
- `cleanPayload_shouldNotDetectThreat`
- `sqlInjectionPayload_shouldDetectThreat`
- `xssPayload_shouldDetectThreat`
- `pathTraversal_shouldDetectThreat`
- `emptyPayload_shouldNotDetectThreat`

---

## Known Limitations

### Fixed Window Rate Limiting (not true sliding window)

The current implementation resets counts at fixed 1-minute intervals, not per-user rolling windows. An attacker can send requests at 11:59 and 12:00, effectively doubling the allowed rate at boundary.

**Production fix:** Redis Sorted Set with `ZADD`/`ZREMRANGEBYSCORE`/`ZCARD` and timestamps as scores.

### Regex-Based Attack Detection Is Bypassable

URL-encoding (`' OR 1=1` → `%27%20OR%201%3D1`) bypasses all regex patterns because the detector sees encoded characters, not the actual attack keywords.

**Production fix:** Normalize input (URL decode → Unicode normalize → strip null bytes) before pattern matching.

### Idempotency Race Condition

Two requests with the same key arriving simultaneously can both pass the `isProcessed` check before either writes `IN_PROGRESS`.

**Production fix:** Redis atomic `SET NX EX` command.

### IP-Based Identity Only

VPN or botnet users can rotate IPs to bypass blacklists.

**Production fix:** Composite identity using `hash(userId + deviceFingerprint + ip)`.

### ddl-auto=update

Hibernate auto-alters schema on startup. Safe for development, dangerous in production.

**Production fix:** Set to `validate` and use Flyway migration scripts.

### /feedback Not Authenticated

Any client can call the feedback endpoint to unblock any IP.

**Production fix:** Protect with admin JWT or internal network access only.
# FinGateway
