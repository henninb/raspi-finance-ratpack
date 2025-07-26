# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a personal finance application built with Ratpack (Groovy), PostgreSQL, and JOOQ. The application provides REST API endpoints for managing financial transactions, accounts, payments, categories, and validation amounts. It uses SSL/HTTPS with a self-signed certificate and runs on port 8443.

## Architecture

### Layered Architecture
- **Handlers**: REST API endpoints defined in `src/ratpack/Ratpack.groovy`
- **Services**: Business logic layer in `src/main/groovy/finance/services/`
- **Repositories**: Data access layer using JOOQ in `src/main/groovy/finance/repositories/` 
- **Domain**: Data models in `src/main/groovy/finance/domain/`

### Key Technologies
- **Ratpack**: Web framework for handling HTTP requests
- **JOOQ**: Type-safe SQL query builder for database operations
- **PostgreSQL**: Primary database (configured to connect to `192.168.10.10:5432/finance_db`)
- **Groovy**: Primary language with `@CompileStatic` annotations
- **Jackson**: JSON serialization/deserialization
- **Spock**: Testing framework

### Domain Entities
Core entities: Transaction, Account, Payment, Category, Description, ValidationAmount, Summary. Each has corresponding service and repository classes following the pattern: `{Entity}Service` and `{Entity}Repository`.

## Development Commands

**Note**: The project has been successfully updated to work with Java 21. All builds now work correctly.

### Build Commands
```bash
./gradlew build          # Build the project
./gradlew shadowJar      # Create fat JAR
./gradlew run           # Run the application
./gradlew clean         # Clean build artifacts
```

### Database Commands
```bash
./gradlew generateJooq      # Generate JOOQ classes from database schema (requires PostgreSQL connection)
```

### Testing
```bash
./gradlew test              # Run tests (Spock-based)
```

### Development Without Database
The project can now be built and compiled without a database connection. JOOQ stub classes are provided in `src/main/java/org/jooq/generated/Tables.java` for compilation purposes. When the database is available, run `./gradlew generateJooq` to replace these with actual generated classes.

## Configuration

### Database Configuration
- Main config: `src/ratpack/db_config.json` (PostgreSQL connection)
- JOOQ config: `build.gradle` jooq block
- Both use same credentials: `henninb/monday1@192.168.10.10:5432/finance_db`

### SSL Configuration
- Certificate: `ssl/hornsup-raspi-finance.jks` 
- Password: `monday1`
- Port: 8443 (HTTPS)

### Version Configuration
All dependency versions are centralized in `gradle.properties`:
- Ratpack: 2.0.0-rc-1 (updated for Java 21 compatibility)
- Groovy: 3.0.21 (compatible with Ratpack 2.0)
- JOOQ: 3.19.13 (latest version)
- PostgreSQL driver: 42.7.4 (latest version)
- Gradle: 8.11 (supports Java 21)

## API Endpoints

Key REST endpoints (all JSON):
- GET `/account/totals` - Account summary
- GET `/transaction/account/select/{accountNameOwner}` - Transactions by account
- POST `/transaction/insert` - Create transaction
- PUT `/transaction/state/update/{guid}/{transactionState}` - Update transaction state
- DELETE `/transaction/delete/{guid}` - Delete transaction
- GET `/categories` - All categories
- POST `/payment/insert` - Create payment

## Development Notes

### Dependency Updates (2025)
The project has been successfully updated for Java 21 compatibility:
- **Ratpack**: Upgraded from 1.10.0-milestone-3 to 2.0.0-rc-1
- **Import Changes**: Ratpack 2.0 moved classes from `ratpack.*` to `ratpack.core.*` package
- **Groovy**: Updated to 3.0.21 (compatible with Ratpack 2.0 and Java 21)
- **JOOQ**: Updated to 3.19.13 with enhanced features
- **Gradle**: Updated to 8.11 for full Java 21 support

### Static Type Checking
Static type checking (@CompileStatic) has been temporarily disabled on repository classes to allow compilation with JOOQ stub classes. Re-enable after running `./gradlew generateJooq` to generate proper JOOQ classes.

### Database Dependencies
Before running the application:
1. Ensure PostgreSQL is running on `192.168.10.10:5432`
2. Database `finance_db` exists with user `henninb` and schema is already set up
3. Run `./gradlew generateJooq` to generate type-safe query classes

### CORS
CORS is handled globally via `CorsHandler` applied to all routes.

### Service Injection
Services use constructor injection via `@Inject` annotation and are bound in the Ratpack bindings block.