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
