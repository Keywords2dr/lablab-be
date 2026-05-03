## 🏗️ Project Architecture

This project follows a standard **3-Tier Architecture** combined with the **Separation of Concerns** principle to ensure maintainability, scalability, and clean code.

### 📁 Directory Structure

```text
src/main/java/com/keywords2dr/lablab/
├── config/       # Application configuration classes
├── controller/   # REST API endpoints (Entry point for client requests)
├── dto/          # Data Transfer Objects (Payloads for Request/Response)
├── entity/       # JPA Entities mapping to database tables
├── exception/    # Global error handling and custom exceptions
├── mapper/       # Object mapping logic (Entity <-> DTO conversions)
├── repository/   # Data access layer (Spring Data JPA interfaces)
├── security/     # Authentication and authorization logic (e.g., JWT)
└── service/      # Core business logic