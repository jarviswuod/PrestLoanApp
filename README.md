# PrestLoan - Loan Prepayment Engine (Category A)

Spring Boot implementation of the interview assessment for **Category A: Prepayment of Principal**.

## Scope Implemented

This project implements all three Category A options:

1. **Option A - Reduce EMI, Keep Tenor**
2. **Option B - Reduce Tenor, Keep EMI**
3. **Option C - Advance Installments (No Recalculation)**

The implementation is generic for any valid installment number, not hardcoded to month 24.

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Data JPA
- MySQL 8
- Flyway migrations
- springdoc-openapi (Swagger UI)
- JUnit 5
- Testcontainers (MySQL integration tests)

## Project Structure

- `src/main/java/com/prestloan/loanengine/api` - REST controllers, request/response DTOs, exception handlers
- `src/main/java/com/prestloan/loanengine/domain` - JPA entities and enums
- `src/main/java/com/prestloan/loanengine/repository` - Spring Data repositories
- `src/main/java/com/prestloan/loanengine/service` - business logic, schedule generator, strategy-style prepayment computations
- `src/main/resources/db/migration` - Flyway SQL migration scripts
- `src/test/java` - unit and integration tests

## Database Setup (Docker)

Run MySQL locally with Docker Compose:

```bash
docker compose up -d
```

The app expects:

- DB: `prestloan`
- User: `prestloan`
- Password: `prestloan`
- Port: `3306`

## Run the Application

If Maven is installed locally:

```bash
mvn spring-boot:run
```

Swagger UI:

- <http://localhost:8080/swagger-ui.html>

OpenAPI JSON:

- <http://localhost:8080/api-docs>

## API Endpoints

All loan endpoints are secured with JWT Bearer authentication.

Default credentials from configuration:

- Username: `admin`
- Password: `admin123`

Generate token first:

`POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Use the returned token as:

`Authorization: Bearer <token>`

### 1) Create Loan

`POST /api/loans`

Sample body:

```json
{
  "principal": 1000000,
  "annualInterestRate": 12.0,
  "tenureMonths": 60,
  "startDate": "2026-01-01"
}
```

### 2) Get Loan

`GET /api/loans/{loanId}`

### 3) Get Current Schedule

`GET /api/loans/{loanId}/schedule`

### 4) Apply Prepayment (Category A)

`POST /api/loans/{loanId}/prepayments`

Sample body for Option A:

```json
{
  "installmentNumber": 24,
  "amount": 200000,
  "option": "REDUCE_EMI_KEEP_TENOR"
}
```

Sample body for Option B:

```json
{
  "installmentNumber": 24,
  "amount": 200000,
  "option": "REDUCE_TENOR_KEEP_EMI"
}
```

Sample body for Option C:

```json
{
  "installmentNumber": 24,
  "amount": 200000,
  "option": "ADVANCE_INSTALLMENTS"
}
```

## Financial Precision and Design Notes

- Monetary calculations use `BigDecimal` and controlled rounding.
- Prepayment logic uses a computation strategy interface (`PrepaymentComputation`) to keep business options modular.
- Schedule updates are transactional.
- Prepayment events are persisted as immutable rows in `loan_transactions`.

## Testing

### Unit tests

- EMI and tenor math checks
- Option A behavior check

### Integration tests

- `LoanControllerIntegrationTest` boots the full app and uses a MySQL Testcontainer.
- Validates loan creation, schedule generation, and prepayment flows for Option A/B/C.

Run tests:

```bash
mvn test
```

If Maven is not installed locally, you can run tests with Docker (Docker daemon required):

```bash
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn test
```

## Deliverables Mapping

1. **Spring Boot source code** - this repository
2. **Database scripts** - `src/main/resources/db/migration/V1__create_loan_tables.sql` and `schema.sql`
3. **Passing test suite** - unit + Testcontainers integration tests under `src/test/java`
4. **README** - this file
