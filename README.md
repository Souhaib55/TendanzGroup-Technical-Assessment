# Insurance Pricing Engine — Tendanz Group Technical Test

## Overview

A full-stack insurance pricing engine that calculates the cost of an insurance policy based on the client's age, geographic zone, and selected product.

**Pricing formula:**
```
Final Price = Base Rate × Age Factor × Zone Risk Coefficient
```

**Example:** Client aged 30, Tunis zone, Auto insurance → `500.00 × 1.00 × 1.20 = 600.00 TND`

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2 · Java 17 · Spring Data JPA · H2 (in-memory) |
| Frontend | Angular 17 · Standalone Components · Reactive Forms |
| Tests | JUnit 5 · @DataJpaTest |
| DevOps | Docker · Docker Compose · GitHub Actions |

## Quick Start

**Backend:**
```bash
cd backend
mvn spring-boot:run
# API: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:testdb)
```

**Frontend:**
```bash
cd frontend
npm install
ng serve
# App: http://localhost:4200
```

**Full Stack with Docker:**
```bash
docker compose up --build
# App: http://localhost
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List all insurance products |
| POST | `/api/quotes` | Create a new quote (returns 201) |
| GET | `/api/quotes/{id}` | Get quote by ID |
| GET | `/api/quotes?productId=X&minPrice=Y` | List quotes with optional filters |

**POST /api/quotes — request body:**
```json
{
  "productId": 1,
  "zoneCode": "TUN",
  "clientName": "Ahmed Ben Ali",
  "clientAge": 30
}
```

**Response includes:** `finalPrice`, `basePrice`, `appliedRules` (full pricing trace)

## Technical Decisions

### Backend Architecture
- **Thin controller, rich service:** `QuoteController` delegates all business logic to `PricingService`. No pricing logic leaks into the HTTP layer.
- **Centralized error handling:** `GlobalExceptionHandler` (@ControllerAdvice) returns consistent JSON errors for validation (400), not-found (404), and unexpected (500) cases.
- **Traceability:** Every quote stores an `appliedRules` JSON array with a human-readable step-by-step pricing breakdown.
- **Precision:** All monetary calculations use `BigDecimal` with `RoundingMode.HALF_UP` to avoid floating-point errors.

### Frontend Architecture
- **Angular 17 Standalone Components** — no NgModule boilerplate.
- **Reactive Forms** for the quote form — full client-side validation matching backend bean validation constraints.
- **Server-side filtering** via `HttpParams` for productId and minPrice filters.
- **In-memory sorting** for date/price columns to avoid extra API calls.

### Testing
- 7 unit tests using `@DataJpaTest` with a real H2 database (no mocking of repositories).
- Covers: all 4 age categories, age boundary conditions (24→25), invalid product ID, invalid zone code.

## Project Structure

```
├── backend/
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/tendanz/pricing/
│       │   ├── controller/   ProductController, QuoteController
│       │   ├── service/      PricingService
│       │   ├── repository/   QuoteRepository (custom queries)
│       │   ├── entity/       Product, Zone, PricingRule, Quote
│       │   ├── dto/          QuoteRequest, QuoteResponse
│       │   ├── enums/        AgeCategory
│       │   └── exception/    GlobalExceptionHandler
│       └── main/resources/   schema.sql, data.sql, application.yml
│
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/app/
│       ├── services/   ProductService, QuoteService
│       ├── pages/      QuoteFormComponent, QuoteListComponent, QuoteDetailComponent
│       └── models/     product.model.ts, quote.model.ts
│
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## CI/CD Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` or `develop`:

1. **backend-ci** — `mvn verify` (build + unit tests) on Java 17
2. **frontend-ci** — `npm ci` + `ng build --configuration production`
3. **docker-build** — builds both images (only on `main`, after jobs 1 & 2 pass)
