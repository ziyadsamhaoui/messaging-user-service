# BadrLink - User Service

**The user and social graph service powering BadrLink, built with Java, Spring Boot, PostgreSQL, and Flyway.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat-square\&logo=springboot\&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square\&logo=openjdk\&logoColor=white)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square\&logo=postgresql\&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat-square\&logo=flyway\&logoColor=white)](https://documentation.red-gate.com/flyway)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square\&logo=docker\&logoColor=white)](https://www.docker.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square\&logo=apachemaven\&logoColor=white)](https://maven.apache.org/)

A dedicated microservice responsible for public user profiles, username search, blocking, and connections. 

---

## Responsibilities

This service owns:

* User profiles
* Username search
* User roles (`USER` / `ADMIN`)
* Blocking and unblocking users
* Connection requests
* Active connections

It **does not store** passwords, emails, tokens, or authentication credentials.

---

## Architecture

The User Service is one of the independent services within BadrLink.

```text
                    ┌─────────────────────┐
                    │    Auth Service     │
                    │      :8081          │
                    └──────────┬──────────┘
                               │
                         JWT / Internal API
                               │
                               ▼
                    ┌─────────────────────┐
                    │    User Service     │
                    │      :8082          │
                    ├─────────────────────┤
                    │ Users               │
                    │ Blocks              │
                    │ Connections         │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   PostgreSQL 17     │
                    │      user_db        │
                    │       :5433         │
                    └─────────────────────┘
```

The service owns its own database and does not share entities or credentials with other services.

Centralized references: [`/docs/API_ENDPOINTS.md`](../docs/API_ENDPOINTS.md), [`/docs/EVENTS.md`](../docs/EVENTS.md), [`/docs/adr/`](../docs/adr/0000-index.md), [`/docs/INCOHERENCES_AND_RESOLUTIONS.md`](../docs/INCOHERENCES_AND_RESOLUTIONS.md).

---

## API

### Users

| Method  | Endpoint           | Description              |
| ------- | ------------------ | ------------------------ |
| `GET`   | `/users/{id}`      | Get a public profile     |
| `GET`   | `/users/search`    | Search users by username |
| `PATCH` | `/users/{id}`      | Update own profile       |
| `PATCH` | `/users/{id}/role` | Change a user's role     |

### Blocks

| Method   | Endpoint            | Description        |
| -------- | ------------------- | ------------------ |
| `POST`   | `/users/{id}/block` | Block a user       |
| `DELETE` | `/users/{id}/block` | Unblock a user     |
| `GET`    | `/users/{id}/block` | List blocked users |

### Connections

| Method | Endpoint                    | Description               |
| ------ | --------------------------- | ------------------------- |
| `POST` | `/users/{id}/connect`       | Send a connection request |
| `POST` | `/connections/{id}/accept`  | Accept a request          |
| `POST` | `/connections/{id}/decline` | Decline a request         |
| `GET`  | `/connections`              | List connections          |
| `GET`  | `/connections/pending`      | List pending requests     |

### Internal

Internal endpoints require the `X-Internal-Token` header (`INTERNAL_HMAC_SECRET`) and are never routed through the Gateway.

| Method | Endpoint                              | Caller           | Description                                             |
| ------ | ------------------------------------- | ---------------- | ------------------------------------------------------- |
| `POST` | `/internal/users`                     | Auth             | Create the profile `{id, username}` from Auth's UUID; idempotent |
| `PATCH`| `/internal/users/{id}/last-seen`      | Realtime Gateway | Disconnect-based `last_seen` update                     |
| `GET`  | `/internal/blocks/check?a=&b=`        | Chat             | Symmetric block check → `{"blocked": bool}`             |

This service is the **source of truth** for usernames and roles: `PATCH /users/{id}` is the only rename path on the platform, and the role change here syncs Auth's denormalized copy through `PATCH /internal/credentials/{id}/role` (rollback + `502` on failure).

## Data Model

The service currently manages three main entities:

```text
User
 ├── Profile information
 └── Role

Block
 └── blocker ↔ blocked

Connection
 ├── PENDING
 ├── ACCEPTED
 └── DECLINED
```

User credentials such as passwords, emails, and tokens are intentionally kept outside this service.

---

## Getting Started

### Requirements

* Java 21
* Docker
* Maven (or the included Maven Wrapper)

### Environment Configuration

Copy the example environment file and configure the required variables:

  ```bash
  cp .env.example .env
  ```

Then update `.env` with your local configuration if needed.

> **Note:** `.env` contains environment-specific values and should not be committed. Use `.env.example` as the template for required variables.

### Start the database

```bash
docker compose up -d
```

### Run the service

```bash
./mvnw spring-boot:run
```

The service will be available at:

```text
http://localhost:8082
```

### Run tests

```bash
./mvnw test
```

Integration tests use **Testcontainers**, so Docker must be running.

---

## Environment Variables

Full template: `.env.example`.

| Variable | Default | Notes |
| -------- | ------- | ----- |
| `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` | localhost/5433/user_db/user_service | PostgreSQL |
| `JWT_JWK_SET_URI` | `http://localhost:8081/oauth2/jwks` | JWKS verification (no endpoint ships yet — INC-02) |
| `JWT_HMAC_SECRET` | empty | HS256 mode for local/dev; when set it takes precedence — **shared** with Auth |
| `INTERNAL_HMAC_SECRET` | — | inbound internal token; **shared** with Auth and Realtime Gateway |
| `AUTH_SERVICE_URL` | `http://localhost:8081` | outbound role sync |
| `KAFKA_BOOTSTRAP_SERVERS / KAFKA_ENABLED` | localhost:9092 / true | event backbone |
| `KAFKA_RELAY_INTERVAL / KAFKA_RELAY_BATCH` | PT0.5S / 100 | outbox relay |

---

## Events (Sprint 6)

Published to `badrlink.user.profile.v1`: `USER_PROFILE_CREATED`, `USER_USERNAME_CHANGED`, `USER_ROLE_CHANGED`, `USER_BLOCKED`, `USER_UNBLOCKED`, `USER_CONNECTION_ACCEPTED`. Consumed from `badrlink.auth.credential.v1`: `CREDENTIAL_REGISTERED` (dual-path registration; calls the same idempotent method as `POST /internal/users`). Catalog: `docs/EVENTS.md`; decision record: [`/docs/adr/0007`](../docs/adr/0007-user-profile-events-dual-path.md).

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/ziyadsamhaoui/messaginguserservice/
│   │       ├── user/
│   │       ├── block/
│   │       ├── connection/
│   │       └── security/
│   └── resources/
│       ├── db/migration/
│       └── application.yml
└── test/
    └── java/
```
