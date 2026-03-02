# Clojure REST API Scaffold Design

**Date**: 2026-03-02
**Purpose**: Team starting point for Clojure REST API projects

## Summary

A production-ready scaffold providing consistent project structure, best practices, and tooling baseline for team use.

## Requirements

- **Database**: PostgreSQL with next.jdbc + HoneySQL
- **Auth**: JWT (buddy-sign + buddy-auth)
- **Example domain**: Users management (CRUD)
- **DevOps**: Docker/docker-compose + GitHub Actions CI
- **Build**: tools.build (build.clj) for uberjar compilation
- **Project structure**: Flat layered (Option A)

## Architecture

```
HTTP Request
    │
    ▼
Ring Middleware Stack
  ├── exception-middleware     ; unified exception handling
  ├── muuntaja                 ; JSON content negotiation
  ├── coercion-middleware      ; Malli request/response validation
  └── jwt-middleware           ; JWT verification (protected routes)
    │
    ▼
Reitit Router
  ├── POST /api/auth/login     ; login, returns JWT
  ├── POST /api/auth/register  ; register
  └── /api/users (protected)
        ├── GET    /           ; list (paginated)
        ├── GET    /:id        ; get by id
        ├── PUT    /:id        ; update
        └── DELETE /:id        ; delete
    │
    ▼
Handler Functions
    │
    ▼
next.jdbc + HoneySQL → PostgreSQL
```

## Tech Stack

| Component | Library |
|-----------|---------|
| Routing | reitit-ring + reitit-coercion-malli |
| HTTP Server | ring-jetty-adapter |
| Serialization | muuntaja + jsonista |
| Validation | malli |
| System lifecycle | integrant + integrant.repl |
| Configuration | aero |
| JWT | buddy-sign + buddy-auth |
| Password hashing | buddy-hashers |
| Database | next.jdbc + hikari-cp |
| SQL building | honey.sql |
| DB migrations | migratus |
| Logging | timbre |
| Testing | kaocha |
| Build | clojure.tools.build.api |

## Project Structure

```
clojure-scaffold/
├── deps.edn
├── build.clj                   ; tools.build uberjar
├── dev/
│   └── user.clj                ; REPL dev entry
├── resources/
│   ├── config.edn              ; Aero config (DB, JWT secret)
│   ├── logback.xml
│   └── migrations/
│       └── 001-create-users.sql
├── src/
│   └── myapp/
│       ├── core.clj            ; -main entry
│       ├── config.clj          ; read config.edn
│       ├── system.clj          ; Integrant system map
│       ├── db/
│       │   ├── core.clj        ; datasource + query helpers
│       │   └── migrations.clj  ; migratus runner
│       ├── routes/
│       │   ├── core.clj        ; route aggregation
│       │   ├── auth.clj        ; auth routes
│       │   └── users.clj       ; user routes
│       ├── handlers/
│       │   ├── auth.clj        ; login/register handlers
│       │   └── users.clj       ; user CRUD handlers
│       ├── middleware/
│       │   └── auth.clj        ; JWT Ring middleware
│       └── models/
│           └── user.clj        ; Malli user schema
├── test/
│   └── myapp/
│       ├── handlers/
│       │   └── users_test.clj
│       └── auth_test.clj
├── Dockerfile
├── docker-compose.yml
└── .github/
    └── workflows/
        └── ci.yml
```

## Data Flow Example (Login)

```
POST /api/auth/login {:email "..." :password "..."}
  → Malli validates request body
  → auth handler: DB query user, bcrypt verify password
  → buddy-sign generates JWT
  → returns {:token "eyJ..."} 200
```

## Build Commands

```bash
# Development REPL
clojure -M:dev

# Run tests
clojure -M:test

# Build uberjar
clojure -T:build uber

# Start with Docker
docker-compose up
```
