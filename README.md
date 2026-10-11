# 🌍 GeoTrack Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%20%2B%20JWT-blue.svg?logo=springsecurity)](https://jwt.io/)
[![Database](https://img.shields.io/badge/Database-MySQL-4479A1.svg?logo=mysql)](https://www.mysql.com/)
[![Deployment](https://img.shields.io/badge/Deploy-Render-46E3B7.svg?logo=render)](https://geo-track-backend.onrender.com)

Production-grade Spring Boot REST API for **GeoTrack** — an enterprise-scale, GPS-geofenced attendance tracking, workforce analytics, and HR management platform.

---

## 🚀 Live Deployment

- **Base URL:** `https://geo-track-backend.onrender.com`
- **Health Check:** `https://geo-track-backend.onrender.com/api/health`
- **Environment:** Render Cloud (Web Service / Dockerized container)

---

## ⚡ Core Features

- **🔐 Robust Authentication & RBAC:**
  - Stateless JWT authentication with access and refresh tokens.
  - Role-Based Access Control across four tiers: `SUPER_ADMIN`, `ORG_ADMIN`, `MANAGER`, and `EMPLOYEE`.
  - BCrypt password hashing.
- **📍 Geofenced Attendance System:**
  - GPS-coordinate validation against designated office locations and customizable geofence radius.
  - Check-in and check-out tracking with status detection (`ON_TIME`, `LATE`, `HALF_DAY`, `ABSENT`).
- **👥 Employee Management (Full CRUD):**
  - Create, view, update, and soft/cascade delete employees.
  - Organization, department, shift, primary office, and hierarchical manager assignments.
  - Managed reference nullification on deletion to preserve relational integrity.
- **📝 Real-Time Work Logs & Daily Summaries:**
  - Daily task tracking with blocker reporting.
  - Individual work logs (`/my`) and organization/team feeds (`/team`) for teammates, managers, and admins.
- **🏖️ Leave Management Workflow:**
  - Leave requests with date ranges and quota calculations.
  - Manager and administrator approval/rejection workflows.
- **🏢 Enterprise Master Data:**
  - Multi-office management with latitude, longitude, and geofence boundary radii.
  - Shift schedules and departmental organization.
- **📡 Real-Time WebSockets:**
  - STOMP over SockJS for live attendance check-in alerts and notification broadcasts.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.2.0
- **Security:** Spring Security 6, JJWT (`0.12.3`)
- **Persistence:** Spring Data JPA, Hibernate 6, MySQL Connector/J
- **Build Tool:** Apache Maven
- **Utilities:** Project Lombok, Jackson (deserialization with case-insensitive enum mapping)
- **Containerization:** Multi-stage Docker build

---

## 📁 Repository Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/geotrack/
│   │   │   ├── config/          # Security, Web, WebSocket & Jackson configurations
│   │   │   ├── controller/      # REST API Controllers (Auth, Employee, WorkLog, etc.)
│   │   │   ├── dto/             # Data Transfer Objects & Request/Response records
│   │   │   ├── entity/          # JPA Domain Entities (Employee, AttendanceLog, etc.)
│   │   │   ├── exception/       # Global exception handler & custom exceptions
│   │   │   ├── repository/     # Spring Data JPA Repositories
│   │   │   ├── security/        # JWT filter, UserDetailsService & token providers
│   │   │   └── service/         # Business logic layer
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🔌 API Reference Highlights

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticates user; returns access token & user profile |
| `POST` | `/api/auth/register` | Public | Onboards a new organization with initial Admin account |
| `POST` | `/api/auth/refresh` | Public | Issues a new access token using a valid refresh token |
| `POST` | `/api/auth/logout` | Authenticated | Revokes current session tokens |

### Employee Management (`/api/employees`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/employees` | Authenticated | Paginated employee directory for caller's organization |
| `POST` | `/api/employees` | Admin / Manager | Creates a new employee with department, shift, & office |
| `GET` | `/api/employees/{id}` | Authenticated | Retrieves detailed employee profile |
| `PUT` | `/api/employees/{id}` | Admin / Manager | Updates employee profile information |
| `DELETE` | `/api/employees/{id}` | Admin / Manager | Deletes employee and safely reassigns subordinates |

### Work Logs (`/api/work-logs`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/work-logs` | Authenticated | Submits today's work summary & blockers |
| `GET` | `/api/work-logs/my` | Authenticated | Retrieves personal work log history |
| `GET` | `/api/work-logs/team` | Authenticated | Retrieves team work log feed |
| `GET` | `/api/work-logs` | Authenticated | Retrieves organization-wide work logs |

### Attendance (`/api/attendance`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/attendance/check-in` | Authenticated | GPS-validated check-in against assigned office geofence |
| `POST` | `/api/attendance/check-out` | Authenticated | Check-out and daily working hours calculation |
| `GET` | `/api/attendance/today` | Authenticated | Current day check-in status |
| `GET` | `/api/attendance/history` | Authenticated | Paginated attendance history |

*Full API definitions and sample payloads are available in [`GeoTrack_API.postman_collection.json`](../GeoTrack_API.postman_collection.json).*

---

## 💻 Local Development Setup

### 1. Prerequisites
- **Java 21 (JDK)**
- **Maven 3.8+**
- **MySQL 8.0+**

### 2. Configure Database
Create a MySQL database:
```sql
CREATE DATABASE geotrack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update your local credentials in `src/main/resources/application-dev.properties` if needed:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/geotrack?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Root
```

### 3. Run Application
```bash
mvn clean spring-boot:run
```
The server will start on **`http://localhost:8085`**.

---

## 🐳 Docker & Production Deployment

### Build and Run with Docker
```bash
docker build -t geotrack-backend .
docker run -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:mysql://host:port/database \
  -e DATABASE_USERNAME=db_user \
  -e DATABASE_PASSWORD=db_password \
  -e JWT_SECRET=your_jwt_secret_key_at_least_256_bits_long \
  geotrack-backend
```

### Render Deployment Configuration
1. Deploy as a **Web Service** using the Docker runtime.
2. Define the following Environment Variables in the Render Dashboard:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: JDBC URL to your MySQL instance (e.g. Aiven, AWS RDS, PlanetScale, or Render MySQL)
   - `DATABASE_USERNAME`: Database username
   - `DATABASE_PASSWORD`: Database password
   - `JWT_SECRET`: 256-bit secret key for HMAC-SHA signing
   - `PORT`: `8085` (or default port)

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
