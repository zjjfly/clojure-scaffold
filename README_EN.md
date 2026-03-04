# Clojure Web Application Scaffold

[中文](README.md) | English

A full-featured Clojure web application scaffold with modern web development essentials.

## Tech Stack

### Core Framework
- **Clojure 1.12.4** - Programming language
- **Ring** - HTTP server abstraction
- **Reitit** - Routing library
- **Integrant** - System component management

### Data Processing
- **Muuntaja** - Content negotiation and format conversion
- **Malli** - Data validation and schema definition

### Authentication & Authorization
- **Buddy Sign** - JWT signing and verification
- **Buddy Hashers** - Password hashing

### Database
- **next.jdbc** - JDBC database access
- **HikariCP** - Connection pooling
- **HoneySQL** - SQL query builder
- **Migratus** - Database migrations
- **PostgreSQL** - Database driver

### Logging
- **Timbre** - Logging framework

### Development Tools
- **Integrant REPL** - REPL-driven development
- **Kaocha** - Testing framework
- **Ring Mock** - HTTP request mocking

## Project Structure

```
clojure-scaffold/
├── src/
│   ├── clj/myapp/          # Application source code
│   │   ├── core.clj        # Application entry point
│   │   ├── system.clj      # System configuration
│   │   ├── config.clj      # Configuration management
│   │   ├── db/             # Database layer
│   │   ├── handlers/       # Request handlers
│   │   ├── middleware/     # Middleware
│   │   └── routes/         # Route definitions
│   ├── java/               # Java source code
│   └── resources/          # Resource files
│       ├── logback.xml     # Logging configuration
│       └── migrations/     # Database migration scripts
├── test/clj/               # Test code
├── dev/                    # Development environment config
├── build/                  # Build scripts
├── scripts/                # Utility scripts
└── deps.edn                # Dependency configuration
```

## Quick Start

### Prerequisites

- Java 11+
- Clojure CLI tools
- PostgreSQL

### Install Dependencies

```bash
clojure -P
```

### Configure Database

1. Create databases:
```bash
createdb myapp_dev
createdb myapp_test
```

2. Configure environment variables (optional, create `.env` file):
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/myapp_dev
```

### Run Database Migrations

```bash
clojure -M:dev -e "(require 'myapp.db.migrations) (myapp.db.migrations/migrate)"
```

### Start Development Server

```bash
clojure -M:dev
```

Then in the REPL:
```clojure
(go)      ; Start system
(reset)   ; Restart system
(halt)    ; Stop system
```

### Run Tests

```bash
clojure -M:test -m kaocha.runner
```

### Build Uberjar

```bash
clojure -X:build uber
```

### Run Production Build

```bash
java -jar target/myapp-standalone.jar
```

## Features

### ✅ User Authentication
- JWT token authentication
- Encrypted password storage
- Login/registration endpoints

### ✅ Database Integration
- PostgreSQL connection pooling
- Database migration management
- HoneySQL query builder

### ✅ RESTful API
- Reitit routing
- Request/response formatting
- Data validation

### ✅ Developer Experience
- REPL-driven development
- Hot reloading
- Comprehensive test suite

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Users
- `GET /api/users` - Get user list (authentication required)
- `GET /api/users/:id` - Get user details (authentication required)

## Development Guide

### REPL Workflow

1. Start REPL: `clojure -M:dev`
2. Load development environment: `(require 'user)`
3. Start system: `(go)`
4. After code changes, restart: `(reset)`

### Adding New Routes

1. Create handler in `src/clj/myapp/handlers/`
2. Define routes in `src/clj/myapp/routes/`
3. Register routes in `src/clj/myapp/system.clj`

### Database Migrations

Create new migration files:
```bash
# Create in src/resources/migrations/ directory
# Format: YYYYMMDDHHMMSS-description.up.sql
#         YYYYMMDDHHMMSS-description.down.sql
```

## Configuration

Application configuration is managed through Aero, supporting environment variables and configuration files.

Configuration file location: `src/resources/config.edn`

## Testing

```bash
# Run all tests
clojure -M:test

# Run specific test
clojure -M:test --focus myapp.handlers.auth-test

# Watch mode
clojure -M:test --watch
```

## Deployment

### Docker (To be implemented)

```bash
docker build -t myapp .
docker run -p 3000:3000 myapp
```

### Environment Variables

- `PORT` - Server port (default: 3000)
- `DATABASE_URL` - Database connection string
- `JWT_SECRET` - JWT signing secret

## License

MIT

## Contributing

Issues and Pull Requests are welcome!
