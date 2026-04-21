# Production Order Monitoring System

A RESTful API built with **Java 21 + Spring Boot 3** for managing industrial production orders — tracking their full lifecycle, execution logs, and providing operational dashboards.

> **Portfolio project** — demonstrates backend engineering skills including REST API design, relational database modeling, explicit SQL queries, Flyway migrations, and clean layered architecture.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Database Design](#database-design)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)

---

## Features

- **Create production orders** with product details, scheduling, priority, and machine assignment
- **Status lifecycle management** with validated transitions (`PENDING → RUNNING → FINISHED / CANCELLED`)
- **Execution log tracking** per order with severity levels (`INFO`, `WARN`, `ERROR`) and JSONB metadata
- **Flexible querying** — filter orders by status, date range, or both, with pagination
- **Dashboard endpoints** — aggregate stats and machine workload reports via raw SQL
- **Automatic DB migrations** via Flyway
- **Consistent error responses** with field-level validation messages

---

## Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Language   | Java 21                           |
| Framework  | Spring Boot 3.2                   |
| Database   | PostgreSQL 15+                    |
| ORM        | Spring Data JPA / Hibernate       |
| Migrations | Flyway                            |
| Build      | Maven                             |
| Utilities  | Lombok                            |
| Testing    | JUnit 5, Spring Boot Test, H2     |

---

## Architecture

```
com.portfolio.productionmonitor
├── controller/     # REST endpoints — request/response handling only
├── service/        # Business logic, status transition rules
├── repository/     # JPA repositories with explicit JPQL and native SQL
├── model/          # JPA entities and enums
├── dto/            # Request and response objects (validation annotations)
├── exception/      # Custom exceptions + GlobalExceptionHandler
└── config/         # (reserved for future: CORS, OpenAPI, security)
```

The project follows a strict **layered architecture**: controllers delegate to services, services use repositories, repositories talk to the database. No business logic leaks into controllers or entities.

---

## Database Design

### Entity Relationship

```
production_orders (1) ──< execution_logs (N)
```

### Table: `production_orders`

| Column          | Type           | Notes                                      |
|-----------------|----------------|--------------------------------------------|
| id              | BIGSERIAL PK   | Auto-generated surrogate key               |
| order_code      | VARCHAR(20)    | Business key — unique, e.g. `OP-2024-0001` |
| product_name    | VARCHAR(150)   | Name of the product being manufactured     |
| quantity        | INTEGER        | Units to produce (`> 0`)                   |
| status          | ENUM           | `PENDING`, `RUNNING`, `FINISHED`, `CANCELLED` |
| priority        | SMALLINT       | 1 (low) to 5 (critical)                   |
| machine_id      | VARCHAR(50)    | Assigned CNC/machine identifier            |
| operator_name   | VARCHAR(100)   | Responsible operator                       |
| notes           | TEXT           | Free-text observations                     |
| scheduled_at    | TIMESTAMPTZ    | Planned start datetime                     |
| started_at      | TIMESTAMPTZ    | Actual start (set on RUNNING transition)   |
| finished_at     | TIMESTAMPTZ    | Actual end (set on FINISHED/CANCELLED)     |
| created_at      | TIMESTAMPTZ    | Immutable — set on insert                  |
| updated_at      | TIMESTAMPTZ    | Auto-updated via DB trigger                |

### Table: `execution_logs`

| Column    | Type        | Notes                                      |
|-----------|-------------|--------------------------------------------|
| id        | BIGSERIAL PK | Auto-generated                            |
| order_id  | BIGINT FK   | References `production_orders(id)`         |
| level     | VARCHAR(10) | `INFO`, `WARN`, `ERROR`                    |
| message   | TEXT        | Human-readable log entry                   |
| metadata  | JSONB       | Optional structured data (machine readings, batch info) |
| logged_at | TIMESTAMPTZ | Immutable — set on insert                  |

### Indexes

| Index                          | Purpose                                 |
|--------------------------------|-----------------------------------------|
| `idx_orders_status`            | Dashboard and status-filter queries     |
| `idx_orders_scheduled_at`      | Period range queries                    |
| `idx_orders_status_scheduled`  | Combined filter (most common pattern)   |
| `idx_orders_machine_id`        | Machine workload reports                |
| `idx_logs_order_id`            | Log fetching per order (FK join)        |
| `idx_logs_logged_at`           | Time-range log queries                  |
| `idx_logs_metadata` (GIN)      | Full JSONB metadata search              |

---

## API Reference

### Base URL
```
http://localhost:8080/api/v1
```

---

### Orders

#### Create an order
```http
POST /orders
Content-Type: application/json

{
  "orderCode": "OP-2024-0010",
  "productName": "Steel Shaft 40mm",
  "quantity": 500,
  "priority": 3,
  "machineId": "CNC-01",
  "operatorName": "John Smith",
  "scheduledAt": "2025-09-01T08:00:00Z"
}
```

#### List orders
```http
GET /orders?status=PENDING&start=2025-01-01T00:00:00Z&end=2025-12-31T23:59:59Z
```

#### Get order by ID
```http
GET /orders/{id}
```

#### Get order by code
```http
GET /orders/code/OP-2024-0001
```

#### Update order status
```http
PATCH /orders/{id}/status
Content-Type: application/json

{
  "status": "RUNNING",
  "machineId": "CNC-01",
  "operatorName": "John Smith"
}
```

**Valid transitions:**
```
PENDING  → RUNNING   | CANCELLED
RUNNING  → FINISHED  | CANCELLED
FINISHED → (terminal)
CANCELLED → (terminal)
```

---

### Execution Logs

#### Add a log entry
```http
POST /orders/{id}/logs
Content-Type: application/json

{
  "level": "WARN",
  "message": "Minor deviation detected on batch #3.",
  "metadata": "{\"batch\": 3, \"deviation_mm\": 0.02}"
}
```

#### List logs for an order
```http
GET /orders/{id}/logs
```

---

### Dashboard

#### Aggregate statistics
```http
GET /orders/dashboard/stats?start=2025-01-01T00:00:00Z&end=2025-12-31T23:59:59Z
```

**Response:**
```json
{
  "total": 120,
  "pending": 30,
  "running": 15,
  "finished": 65,
  "cancelled": 10,
  "avgDurationHours": 4.73
}
```

#### Machine workload report
```http
GET /orders/dashboard/machines
```

**Response:**
```json
[
  {
    "machineId": "CNC-01",
    "totalOrders": 45,
    "completed": 38,
    "inProgress": 3,
    "avgQuantity": 312.5
  }
]
```

---

### Error Responses

All errors follow a consistent format:

```json
{
  "status": 404,
  "message": "Order not found: 99",
  "timestamp": "2025-08-20T14:32:00Z"
}
```

Validation errors include field-level detail:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "orderCode": "Order code must follow pattern OP-YYYY-NNNN",
    "quantity": "Quantity must be at least 1"
  },
  "timestamp": "2025-08-20T14:32:00Z"
}
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- (Optional) Docker

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/production-order-monitor.git
cd production-order-monitor
```

### 2. Create the database

```sql
CREATE DATABASE production_monitor;
```

### 3. Configure environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/production_monitor
export DB_USER=postgres
export DB_PASSWORD=your_password
```

Or create an `.env` file and load it before running.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

Flyway will automatically run all migrations on startup. Seed data is included.

### 5. Test the API

```bash
# Create an order
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
{
  "orderCode": "OP-2026-0001",
  "productName": "Steel Shaft 40mm",
  "quantity": 120,
  "priority": 3,
  "machineId": "JXB-012",
  "operatorName": "Pedro Rafael",
  "scheduledAt": "2026-04-21T14:00:00Z"
}

# List all orders
http://localhost:8080/api/v1/orders

# Get dashboard stats
http://localhost:8080/api/v1/orders/dashboard/stats
```

---

## Environment Variables

| Variable      | Default                                            | Description              |
|---------------|----------------------------------------------------|--------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/production_monitor` | JDBC connection URL   |
| `DB_USER`     | `postgres`                                         | Database username         |
| `DB_PASSWORD` | `postgres`                                         | Database password         |
| `SERVER_PORT` | `8080`                                             | HTTP server port          |

---

## Running Tests

```bash
./mvnw test
```

Tests use an in-memory **H2** database — no PostgreSQL required for the test suite.

---

## Project Structure

```
production-order-monitor/
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/productionmonitor/
│   │   │   ├── ProductionMonitorApplication.java
│   │   │   ├── controller/
│   │   │   │   └── ProductionOrderController.java
│   │   │   ├── service/
│   │   │   │   └── ProductionOrderService.java
│   │   │   ├── repository/
│   │   │   │   ├── ProductionOrderRepository.java
│   │   │   │   └── ExecutionLogRepository.java
│   │   │   ├── model/
│   │   │   │   ├── ProductionOrder.java
│   │   │   │   ├── ExecutionLog.java
│   │   │   │   ├── OrderStatus.java
│   │   │   │   └── LogLevel.java
│   │   │   ├── dto/
│   │   │   │   └── OrderDTOs.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       └── InvalidStatusTransitionException.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           └── V1__create_initial_schema.sql
│   └── test/
│       └── java/com/portfolio/productionmonitor/
├── pom.xml
└── README.md
```

---

## Author

Built as part of a backend engineering portfolio.  
Focus: clean architecture, SQL proficiency, and production-grade API design.
