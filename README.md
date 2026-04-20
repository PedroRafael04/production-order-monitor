# Production Order Monitoring API

REST API for managing and monitoring production orders.  
Built with Spring Boot, following a layered architecture and best practices for maintainability and scalability.

---

## Overview

This application provides endpoints to:

- Create and manage production orders
- Track execution logs
- Update order status
- Retrieve aggregated dashboard statistics

The project emphasizes clean separation of concerns, database versioning, and structured API design.

---

## Tech Stack

- Java 17+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway (database migrations)
- Lombok
- H2 (configured for test profile)
- Maven

---

## Architecture

The project follows a layered architecture:

- Controller → Handles HTTP requests and responses
- Service → Contains business logic
- Repository → Data access layer (JPA)
- DTO → Data transfer between layers
- Model (Entity) → Database representation
- Exception → Global error handling

Business logic is concentrated in the service layer, with controllers acting as thin entry points.

---

## Database

Database schema is managed via Flyway migrations.

Main structures include:

- production_order
- execution_logs
- order_status (ENUM)
- Indexes for performance optimization
- Trigger for automatic updated_at management

---

## Environment Configuration

The application supports environment variables.

Example .env:

DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=postgres
DB_PASSWORD=yourpassword

Note:
The application currently uses default port 8080.

To support dynamic port configuration, update:

server.port=${SERVER_PORT:8080}

---

## Running the Application

Using Maven:

mvn spring-boot:run

Or build and run:

mvn clean install
java -jar target/*.jar

---

## API Endpoints

Production Orders:

POST /api/v1/orders
GET /api/v1/orders
GET /api/v1/orders/id/{id}
PATCH /api/v1/orders/{id}/status

---

Dashboard:

GET /api/v1/dashboard/stats?start={date}&end={date}

---

## Testing

The project includes unit tests using Mockito.

Although H2 is configured for a test profile, current tests focus on service-layer validation and do not yet cover full integration with the database.

---

## Error Handling

Global exception handling is implemented using @ControllerAdvice.

---

## Project Structure

src/
 ├── controller/
 ├── service/
 ├── repository/
 ├── dto/
 ├── model/
 ├── exception/
 └── config/

---

## Version Control Strategy

- feat → New features
- fix → Bug fixes
- chore → Maintenance tasks
- docs → Documentation changes

---

## Current Limitations

- Dashboard query relies on positional mapping (Object[])
- Integration tests with H2 are not yet implemented
- Port configuration is not fully environment-driven

---

## Future Improvements

- Introduce integration tests with H2
- Replace Object[] with typed DTO projections
- Improve API contract consistency
- Add Swagger/OpenAPI documentation
- Implement pagination and filtering

---

## Author

Pedro Rafael
