# BloodConnect Backend — JUnit + Docker + Redis Upgrade Guide

This document covers the three additions made to the Spring Boot backend and how
to run/verify each. Everything here is real and runnable — nothing is stubbed.

---

## 1. JUnit Testing

### What was added
A test suite under `src/test/java/com/bloodconnect/`:

| Test class | What it proves |
|---|---|
| `util/BloodCompatTest` | Blood-type compatibility matrix (O- universal donor, AB+ universal recipient, per-type counts) |
| `util/TokensTest` | SHA-256 hashing is deterministic; random reset tokens are unique |
| `security/JwtUtilTest` | JWT sign/parse round-trip; tampered, foreign-secret, and expired tokens are rejected |
| `service/AiServiceTest` | Groq AI failures fall back to safe defaults instead of crashing |
| `controller/AuthControllerTest` | Register (happy path + duplicate-email 409), login (success + bad-credentials 401) — using Mockito |
| `controller/DonorControllerTest` | Leaderboard mapping + Haversine radius filtering on `/nearby` — using Mockito |

The controller tests use **Mockito** (`@Mock`, `when(...).thenReturn(...)`,
`verify(...)`) to isolate the controller from the database. The rest are plain
JUnit 5 unit tests. No new dependencies were needed — `spring-boot-starter-test`
already bundles JUnit 5, Mockito, and AssertJ.

### How to run
```bash
mvn test
```

### Expected output (tail)
```
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
(31 = individual test executions; the 8 parameterized blood-type cases count separately.)

To run a single class:
```bash
mvn test -Dtest=BloodCompatTest
```

---

## 2. Docker

### Files added
- **`Dockerfile`** — multi-stage build. Stage 1 builds the jar with Maven (Java 17);
  stage 2 copies just the jar into a slim JRE image and runs as a non-root user.
- **`.dockerignore`** — keeps `target/`, `.git`, `.env`, etc. out of the build context.
- **`docker-compose.yml`** — runs Postgres + Redis + the backend together, wired up
  with health checks so the app only starts once the database and cache are ready.

### Run the whole stack (recommended)
From the `backend/` folder:
```bash
# optional: create a .env file (copy from .env.example) for secrets like GROQ_API_KEY
docker compose up --build
```
This starts:
- `postgres` on `localhost:5432`
- `redis` on `localhost:6379`
- `backend` on `localhost:8080` (REST) and `localhost:5000` (Socket.IO)

Your React frontend keeps running outside Docker (`npm run dev`) and still proxies
to `localhost:8080` exactly as before.

Stop everything:
```bash
docker compose down          # keep data
docker compose down -v       # also wipe the Postgres volume
```

### Build just the backend image
```bash
docker build -t bloodconnect-backend .
```

---

## 3. Redis Caching

### What was added
- `spring-boot-starter-data-redis` + `spring-boot-starter-cache` in `pom.xml`
- `config/CacheConfig.java` — `@EnableCaching` + a `RedisCacheManager` with JSON
  serialization and per-cache TTLs
- `@Cacheable` / `@CacheEvict` annotations on the hot read paths

### What is cached and why

| Cache | Where | TTL | Reason |
|---|---|---|---|
| `leaderboard` | `DonorController.leaderboard()` | 5 min | Read often, changes rarely |
| `nearbyDonors` | `DonorController.nearby()` | 2 min | Geo search is repeated a lot; slight staleness is fine |
| `healthTips` | `AiService.generateHealthTip()` | 1 hour | Avoids paying expensive Groq API latency for identical donor profiles |

Caches are **evicted** automatically when a donor profile is updated
(`PUT /api/donors/profile`) or verified (`PUT /api/donors/{id}/verify`), so cached
data never goes silently stale.

### Verify the cache is working
1. Start Redis (via `docker compose up`, or `docker run -p 6379:6379 redis:7-alpine`).
2. Hit the leaderboard twice:
   ```bash
   curl http://localhost:8080/api/donors/leaderboard      # first call: hits Postgres
   curl http://localhost:8080/api/donors/leaderboard      # second call: served from Redis
   ```
3. Look inside Redis:
   ```bash
   docker exec -it bloodconnect-redis redis-cli
   > KEYS *
   1) "leaderboard::SimpleKey []"
   > TTL "leaderboard::SimpleKey []"
   (integer) 287          # seconds left before it expires
   ```

### Running locally WITHOUT Redis
If you just want to run the app without a Redis server:
```bash
CACHE_TYPE=none mvn spring-boot:run
```
(or set `CACHE_TYPE=none` in your env / run config). The app boots normally with
caching disabled.

---

## Resume lines you can now defend

Pick whichever fit your resume style — every word is backed by code you can walk
an interviewer through:

- *Wrote a JUnit 5 + Mockito test suite (unit tests for JWT auth, blood-type
  compatibility logic, and REST controllers) covering happy-path and failure cases.*
- *Containerized the Spring Boot service with a multi-stage Docker build and a
  Docker Compose stack (app + PostgreSQL + Redis) with health-check-gated startup.*
- *Added a Redis caching layer (Spring Cache abstraction) for read-heavy donor
  search, leaderboard, and AI-generated health-tip endpoints, with per-cache TTLs
  and write-through eviction.*

### Interview-ready talking points
- **Why cache the leaderboard?** It's read on every dashboard load but only changes
  when a donation is recorded — classic high-read/low-write, perfect for a short TTL.
- **How do you stop stale cache data?** Two mechanisms: a TTL so entries expire on
  their own, plus explicit `@CacheEvict` when a donor profile changes.
- **Why multi-stage Docker?** The final image ships only a JRE + the jar — no Maven,
  no build tools — so it's smaller and has a smaller attack surface.
- **Why Mockito for controllers?** To test request-handling logic (validation, status
  codes, branching) without spinning up a database.
