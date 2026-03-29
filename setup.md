# 📑 Doc-Management-Api - Setup Guide

Modern Document Management API built with Spring Boot 4.0.2, focused on compliance and advanced backup solutions.

---

## 🚀 Quick Start with Docker

The easiest way to run the full stack (API + MySQL) is using Docker Compose. This handles all dependencies, network setup, and initial database state automatically.

```bash
docker-compose up -d --build
```

### Infrastructure Details:
- **Database**: Port `3306`, DB Name `doc_manager_db`.
- **API**: Port `8080`.
- **Persistence**: 
    - `mysql_data`: Volume for database data.
    - `./uploads`: Host directory mounted for document storage.
    - `./backups`: Host directory mounted for backup archives.

---

## 🛠 Manual Development Setup

If you prefer to run the application locally (without Docker), follow these steps:

### 1. Prerequisites
- **JDK 17** or higher.
- **Maven 3.8+**.
- **MySQL/MariaDB** instance running.
- **Environment Tools**: `mysqldump` (or `mariadb-dump`) must be in your system's PATH for database backups to function.

### 2. Configuration (`.properties`)
Create files from the provided templates:
1. `src/main/resources/application.properties` (Global configuration and JWT keys).
2. `src/main/resources/application-dev.properties` (Local dev database).
3. `src/main/resources/application-prod.properties` (Production environment).

> [!TIP]
> Ensure `security.jwt.secret-key` is set to a strong, random string in your local properties.

### 3. Running the Application

**Development Mode** (Auto-updates schema, Flyway disabled):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production Mode** (Validates schema, Flyway migrations enabled):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 💾 Backup & Recovery System

The API includes a robust backup system triggered via scheduler (daily at midnight) or manually via API.

- **Manual Full Backup**: `POST /api/v1/backups/full`
- **File Recovery**: If a file is missing, use `/api/v1/backups/recover/{warningId}` to restore it from the ZIP archives.
- **Storage Paths**: Configured via `app.backup.*` properties. By default, backups are saved in the `backups/` project subdirectory.

---

## 📚 API Documentation

Interactive documentation is automatically generated and available when the application is running:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> [!IMPORTANT]
> To test protected endpoints in Swagger UI, use the **Authorize** button and provide your JWT token in the format: `Bearer <your_token>`.

---

## 🗂 Database Migrations (Flyway)

In **Production** mode, schema changes are managed via Flyway. 
- Migration scripts are located in `src/main/resources/db/migration`.
- The application will automatically execute any pending scripts on startup. 
- Ensure your production database is clean or `baseline-on-migrate` is correctly configured.
