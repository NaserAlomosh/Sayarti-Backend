# Sayarti Backend

Backend REST API for **Sayarti**, a vehicle management application that allows users to manage vehicles, fuel consumption, maintenance, expenses, reminders, notifications, and vehicle statistics.

This repository uses this README as the main:

- Product scope.
- Technical roadmap.
- Development checklist.
- Implementation progress tracker.

Every feature must remain unchecked until it is fully implemented, tested, documented, and verified.

---

# Project Status

```text
Project: Sayarti Backend
Version: V1
Status: In Development
```

## Current Foundation

The Java 17 Maven/Spring Boot foundation now includes local email/password authentication, hashed passwords, JWT access authentication, rotating hashed refresh tokens, logout, current-user resolution, and backend-verified Google ID-token authentication. Google sessions use the same JWT and refresh-token rotation as local sessions.

Microsoft SQL Server is the only database engine used by the project. H2 is not used. Production and development use Microsoft SQL Server directly, while database-backed integration tests use isolated Microsoft SQL Server instances through Testcontainers.

The authentication schema is managed by Flyway and its HTTP contract is documented with OpenAPI. No vehicle or other V1 business feature has been implemented yet.

Docker Desktop and Testcontainers connectivity have been verified locally. Testcontainers successfully detects Docker through the local Unix socket and can pull its supporting containers and the Microsoft SQL Server 2022 image.

The full Maven verification remains incomplete until the SQL Server-backed test suite finishes successfully and Maven reports `BUILD SUCCESS`.

Checklist items below remain unchecked where completion still depends on owner-provided configuration, live external services, unfinished Docker Compose configuration, complete SQL Server integration verification, or unimplemented V1 business features.

---

# Technology Stack

The backend must use:

- Java 17
- Spring Boot
- Maven
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- Microsoft SQL Server
- Flyway
- JWT
- OAuth 2.0
- Google Authentication
- Bean Validation
- OpenAPI / Swagger
- Firebase Admin SDK
- Docker
- JUnit 5
- Mockito
- Testcontainers

Microsoft SQL Server is used for application persistence and database-backed integration testing. H2 must not be introduced as a replacement integration-test database.

---

# Core Development Rules

Codex must follow these rules during development:

1. Read this entire README before implementing features.
2. Read `AGENTS.md` before making code changes.
3. Read `SECRETS_SETUP.md` before configuring external integrations.
4. Work one logical feature at a time.
5. Do not implement features outside the V1 scope.
6. Do not mark partially implemented features as complete.
7. Change `- [ ] Feature` to `- [x] Feature` only after the Definition of Done is satisfied.
8. Every external key, secret, credential, certificate, client ID, token, or configuration value that cannot be generated locally must be documented in `SECRETS_SETUP.md`.
9. Never commit actual secrets to Git.
10. Never hardcode secrets in Java source files.
11. Microsoft SQL Server is the only supported relational database engine.
12. Do not introduce H2 for integration testing.
13. Database-backed integration tests must use Microsoft SQL Server Testcontainers.
14. Unit tests that do not require persistence must not start database containers.
15. Never point automated tests at development or production databases.

---

# Definition of Done

A checklist item may only be marked complete after all applicable requirements are satisfied:

```text
Implementation
+
Validation
+
Authorization
+
Error Handling
+
Database Migration
+
Swagger Documentation
+
Unit Tests
+
SQL Server Integration Tests where applicable
+
Build Verification
```

If any required part is missing, the feature stays unchecked.

---

# 1. Initial Project Setup

- [x] **Initialize Spring Boot Project**

Create the Spring Boot project using Java 17 and Maven.

Base package:

```text
com.sayarti.backend
```

Main class:

```text
SayartiApplication
```

- [x] **Add Core Dependencies**

Required dependencies:

```text
Spring Web
Spring Security
Spring Data JPA
Validation
Microsoft SQL Server JDBC Driver
Flyway Core
Flyway SQL Server
Lombok
OpenAPI / Swagger
JWT
OAuth2 Client
Firebase Admin SDK
Spring Boot Test
Spring Boot Testcontainers
Testcontainers JUnit Jupiter
Testcontainers Microsoft SQL Server
```

- [ ] **Configure Maven**

Configure:

```text
Java 17
UTF-8
Spring Boot Maven Plugin
```

The following command must succeed before this item is considered complete:

```bash
./mvnw clean verify
```

---

# 2. Application Configuration

- [x] **Create Application Configuration**

Create:

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
src/test/resources/application-test.yml
```

- [x] **Configure Environment Variables**

Environment-specific and sensitive values must use environment variables.

Examples:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD

JWT_ACCESS_SECRET
JWT_REFRESH_SECRET
JWT_ACCESS_EXPIRATION
JWT_REFRESH_EXPIRATION

GOOGLE_CLIENT_ID

FIREBASE_PROJECT_ID
FIREBASE_CLIENT_EMAIL
FIREBASE_PRIVATE_KEY
```

Any variable requiring input from the project owner must be documented in `SECRETS_SETUP.md`.

- [ ] **Create `.env.example`**

```env
DB_HOST=localhost
DB_PORT=1433
DB_NAME=sayarti
DB_USERNAME=
DB_PASSWORD=

JWT_ACCESS_SECRET=
JWT_REFRESH_SECRET=

JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000

GOOGLE_CLIENT_ID=

FIREBASE_PROJECT_ID=
FIREBASE_CLIENT_EMAIL=
FIREBASE_PRIVATE_KEY=
```

Never add real credentials.

Copy `.env.example` to an ignored `.env` for Docker Compose, or export the same variables in the shell when running the application directly. Spring Boot does not read `.env` files by itself.

---

# 3. Microsoft SQL Server

- [x] **Configure SQL Server Connection**

Default development settings:

```text
Host: localhost
Port: 1433
Database: sayarti
```

Example JDBC URL:

```text
jdbc:sqlserver://${DB_HOST}:${DB_PORT};databaseName=${DB_NAME};encrypt=true;trustServerCertificate=true
```

- [x] **Add Microsoft SQL Server JDBC Driver**

Use:

```text
com.microsoft.sqlserver:mssql-jdbc
```

- [x] **Configure Hibernate**

Use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Never use `ddl-auto=update` as the database migration strategy. Schema changes must be managed through Flyway.

### Database Testing Policy

Database-backed integration tests must use an isolated Microsoft SQL Server instance through Testcontainers instead of H2.

This allows tests to detect SQL Server-specific behavior involving SQL syntax, column types, constraints, indexes, Flyway migrations, Hibernate mappings, and database behavior.

Unit tests that do not require persistence must not start a SQL Server container.

---

# 4. Flyway Database Migrations

- [x] **Configure Flyway**

Migration location:

```text
src/main/resources/db/migration
```

Naming convention:

```text
V1__create_users.sql
V2__create_refresh_tokens.sql
V3__create_vehicles.sql
```

- [ ] **Create Initial Database Schema**

Required tables:

- [x] `users`
- [x] `refresh_tokens`
- [ ] `vehicles`
- [ ] `fuel_records`
- [ ] `maintenance_records`
- [ ] `expenses`
- [ ] `reminders`
- [ ] `devices`

All schema changes must be performed through Flyway migrations.

---

# 5. Project Architecture

- [x] **Create Feature-Based Architecture**

Target structure:

```text
src/main/java/com/sayarti/backend/

├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── repository/
│   ├── entity/
│   └── mapper/
├── user/
├── vehicle/
├── fuel/
├── maintenance/
├── expense/
├── reminder/
├── statistics/
├── notification/
├── device/
├── security/
│   ├── config/
│   ├── jwt/
│   ├── oauth/
│   └── filter/
├── common/
│   ├── exception/
│   ├── response/
│   ├── pagination/
│   ├── auditing/
│   └── util/
├── config/
└── SayartiApplication.java
```

---

# 6. Architecture Rules

- [ ] Controllers contain HTTP-related logic only.
- [ ] Business logic belongs in services.
- [ ] Database access goes through repositories.
- [ ] JPA entities are never returned directly from controllers.
- [ ] Request DTOs are used for client input.
- [ ] Response DTOs are used for client output.
- [ ] Constructor injection must be used.
- [ ] Field injection must not be used.
- [ ] Shared logic must not be duplicated.
- [ ] Business exceptions must be explicit and meaningful.

Do not use field injection with `@Autowired`.

---

# 7. Common API Response

- [x] **Create Standard Success Response**
- [x] **Create Standard Error Response**
- [ ] **Create Pagination Response**
- [ ] **Create Sorting Support**
- [ ] **Create Date Range Filtering**

Success example:

```json
{"success":true,"data":{},"message":null}
```

Error example:

```json
{"success":false,"error":{"code":"VEHICLE_NOT_FOUND","message":"Vehicle not found"}}
```

---

# 8. Global Error Handling

- [x] **Create Global Exception Handler**

Use `@RestControllerAdvice`.

Handle validation errors, authentication errors, authorization errors, resource not found, business validation errors, database errors, and unexpected errors.

- [x] **Create Error Code Enum**

Initial codes:

```text
VALIDATION_ERROR
UNAUTHORIZED
FORBIDDEN
INTERNAL_SERVER_ERROR
AUTH_INVALID_CREDENTIALS
AUTH_EMAIL_ALREADY_EXISTS
AUTH_TOKEN_EXPIRED
AUTH_INVALID_TOKEN
AUTH_INVALID_REFRESH_TOKEN
AUTH_GOOGLE_LOGIN_FAILED
USER_NOT_FOUND
VEHICLE_NOT_FOUND
VEHICLE_ACCESS_DENIED
INVALID_VEHICLE_MILEAGE
FUEL_RECORD_NOT_FOUND
INVALID_FUEL_RECORD
MAINTENANCE_NOT_FOUND
EXPENSE_NOT_FOUND
REMINDER_NOT_FOUND
```

---

# 9. API Versioning

- [x] **Configure API Base Path**

All V1 APIs must start with `/api/v1`.

---

# 10. Swagger / OpenAPI

- [x] **Configure Swagger**
- [x] **Document All APIs**

Swagger UI should be available in development at `/swagger-ui/index.html`.

---

# 11. Security Configuration

- [x] **Configure Spring Security**
- [x] **Configure Stateless Authentication**
- [x] **Configure Public Routes**
- [x] **Configure Password Encoder**
- [x] **Configure CORS**
- [x] **Configure Security Headers**

Use `SecurityFilterChain` and `SessionCreationPolicy.STATELESS`.

Public routes:

```text
/api/v1/auth/**
/swagger-ui/**
/v3/api-docs/**
```

---

# 12. JWT Authentication

- [x] **Create JWT Service**
- [x] **Create JWT Authentication Filter**
- [x] **Configure Access Token Expiration**
- [x] **Configure Refresh Token Expiration**
- [x] **Create Refresh Token Storage**
- [x] **Implement Refresh Token Rotation**

Access token header:

```http
Authorization: Bearer <access-token>
```

Recommended starting expirations:

```text
Access Token: 15 minutes
Refresh Token: 30 days
```

Store refresh tokens securely, preferably hashed.

---

# 13. User Authentication

- [x] **Register User** — `POST /api/v1/auth/register`
- [x] **Login With Email and Password** — `POST /api/v1/auth/login`
- [x] **Refresh Session** — `POST /api/v1/auth/refresh`
- [x] **Logout** — `POST /api/v1/auth/logout`

Registration requires first name, last name, valid unique email, password validation, password hashing, and DTO validation.

Login returns access token, refresh token, and user information.

Logout must invalidate the refresh token.

---

# 14. Google OAuth 2.0

- [x] **Configure Google OAuth 2.0**
- [x] **Implement Google Login Flow**
- [x] **Verify Google Identity Token**
- [x] **Handle Existing Users / Account Linking**

The mobile application obtains a Google ID token using the `openid`, `email`, and `profile` scopes and sends only that token to `POST /api/v1/auth/google` as `{"idToken":"<google-id-token>"}`.

The backend verifies Google's signature, issuer, `GOOGLE_CLIENT_ID` audience, expiration, subject, and verified email. Names and email are read exclusively from verified token claims.

Automatic linking to an existing `LOCAL` account with the same verified email is intentionally forbidden. The endpoint returns `409 AUTH_ACCOUNT_LINKING_REQUIRED`.

No `GOOGLE_CLIENT_SECRET` is used or required because the backend does not exchange an authorization code.

---

# 15. User Profile

Fields:

```text
id
firstName
lastName
email
passwordHash
authProvider
createdAt
updatedAt
deletedAt
```

Providers: `LOCAL`, `GOOGLE`.

- [x] **Create User Entity**
- [x] **Get Current User** — `GET /api/v1/users/me`
- [ ] **Update Current User** — `PATCH /api/v1/users/me`
- [ ] **Delete Account** — `DELETE /api/v1/users/me`

---

# 16. Vehicle Management

Fields:

```text
id
userId
brand
model
year
licensePlate
fuelType
currentMileage
nickname
imageUrl
createdAt
updatedAt
deletedAt
```

Fuel types:

```text
GASOLINE_90
GASOLINE_95
GASOLINE_98
DIESEL
HYBRID
ELECTRIC
OTHER
```

- [ ] Create Vehicle Entity
- [ ] Create Vehicle Repository
- [ ] Create Vehicle DTOs
- [ ] Create Vehicle — `POST /api/v1/vehicles`
- [ ] Get User Vehicles — `GET /api/v1/vehicles`
- [ ] Get Vehicle Details — `GET /api/v1/vehicles/{vehicleId}`
- [ ] Update Vehicle — `PATCH /api/v1/vehicles/{vehicleId}`
- [ ] Update Mileage — `PATCH /api/v1/vehicles/{vehicleId}/mileage`
- [ ] Delete Vehicle — `DELETE /api/v1/vehicles/{vehicleId}`

Mileage must not normally decrease. Use soft delete.

---

# 17. Vehicle Ownership Security

- [ ] **Create Vehicle Ownership Validation**
- [ ] Protect Vehicle APIs
- [ ] Protect Fuel APIs
- [ ] Protect Maintenance APIs
- [ ] Protect Expense APIs
- [ ] Protect Reminder APIs
- [ ] Protect Statistics APIs
- [ ] Protect Dashboard APIs

Every vehicle-owned resource must validate ownership against the authenticated user.

---

# 18. Fuel Tracking

Fields:

```text
id
vehicleId
mileage
liters
totalCost
pricePerLiter
fuelType
isFullTank
date
notes
createdAt
updatedAt
```

- [ ] Create Fuel Entity
- [ ] Create Fuel Repository
- [ ] Create Fuel DTOs
- [ ] Create Fuel Record — `POST /api/v1/vehicles/{vehicleId}/fuel`
- [ ] Get Fuel History — `GET /api/v1/vehicles/{vehicleId}/fuel`
- [ ] Get Fuel Record — `GET /api/v1/vehicles/{vehicleId}/fuel/{fuelId}`
- [ ] Update Fuel Record — `PATCH /api/v1/vehicles/{vehicleId}/fuel/{fuelId}`
- [ ] Delete Fuel Record — `DELETE /api/v1/vehicles/{vehicleId}/fuel/{fuelId}`

Fuel history supports pagination, sorting, and date filtering.

---

# 19. Fuel Calculations

- [ ] Calculate Distance Between Refills
- [ ] Calculate Fuel Efficiency
- [ ] Calculate L/100km
- [ ] Calculate Cost Per Kilometer
- [ ] Calculate Monthly Fuel Cost
- [ ] Calculate Total Fuel Cost
- [ ] Calculate Average Fuel Efficiency

```text
Distance = Current Odometer - Previous Odometer
km/L = Distance / Liters
L/100km = (Liters / Distance) × 100
Fuel Cost Per KM = Fuel Cost / Distance
```

---

# 20. Maintenance

Types:

```text
ENGINE_OIL
OIL_FILTER
AIR_FILTER
CABIN_FILTER
BRAKES
TIRES
BATTERY
TRANSMISSION
COOLANT
SPARK_PLUGS
GENERAL_SERVICE
OTHER
```

- [ ] Create Maintenance Entity
- [ ] Create Maintenance Repository
- [ ] Create Maintenance DTOs
- [ ] Create Maintenance Record — `POST /api/v1/vehicles/{vehicleId}/maintenance`
- [ ] Get Maintenance History — `GET /api/v1/vehicles/{vehicleId}/maintenance`
- [ ] Get Maintenance Record — `GET /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}`
- [ ] Update Maintenance Record — `PATCH /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}`
- [ ] Delete Maintenance Record — `DELETE /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}`

---

# 21. Expenses

Categories:

```text
INSURANCE
LICENSE
PARKING
CAR_WASH
TOLLS
FINES
ACCESSORIES
LOAN_PAYMENT
OTHER
```

- [ ] Create Expense Entity
- [ ] Create Expense Repository
- [ ] Create Expense DTOs
- [ ] Create Expense — `POST /api/v1/vehicles/{vehicleId}/expenses`
- [ ] Get Expenses — `GET /api/v1/vehicles/{vehicleId}/expenses`
- [ ] Get Expense — `GET /api/v1/vehicles/{vehicleId}/expenses/{expenseId}`
- [ ] Update Expense — `PATCH /api/v1/vehicles/{vehicleId}/expenses/{expenseId}`
- [ ] Delete Expense — `DELETE /api/v1/vehicles/{vehicleId}/expenses/{expenseId}`

---

# 22. Reminders

Types:

```text
LICENSE
INSURANCE
ENGINE_OIL
MAINTENANCE
TIRES
BATTERY
CUSTOM
```

- [ ] Create Reminder Entity
- [ ] Create Reminder Repository
- [ ] Create Reminder DTOs
- [ ] Create Reminder — `POST /api/v1/vehicles/{vehicleId}/reminders`
- [ ] Get Reminders — `GET /api/v1/vehicles/{vehicleId}/reminders`
- [ ] Update Reminder — `PATCH /api/v1/vehicles/{vehicleId}/reminders/{reminderId}`
- [ ] Complete Reminder — `PATCH /api/v1/vehicles/{vehicleId}/reminders/{reminderId}/complete`
- [ ] Delete Reminder — `DELETE /api/v1/vehicles/{vehicleId}/reminders/{reminderId}`

Support date-based, mileage-based, and combined reminders.

---

# 23. Device Management

Platforms: `ANDROID`, `IOS`.

- [ ] Create Device Entity
- [ ] Create Device Repository
- [ ] Create Device DTOs
- [ ] Register Device — `POST /api/v1/devices`
- [ ] Update FCM Token
- [ ] Delete Device

---

# 24. Firebase Cloud Messaging

- [ ] Configure Firebase Admin SDK
- [ ] Create Notification Service
- [ ] Send Notification To Device
- [ ] Send Notification To User Devices
- [ ] License Expiration Notification
- [ ] Insurance Expiration Notification
- [ ] Maintenance Notification
- [ ] Mileage Reminder Notification
- [ ] Custom Reminder Notification

Any required Firebase credential must be documented in `SECRETS_SETUP.md`.

---

# 25. Reminder Scheduler

- [ ] Enable Scheduling
- [ ] Create Reminder Scheduler
- [ ] Check Date-Based Reminders
- [ ] Check Mileage-Based Reminders
- [ ] Prevent Duplicate Notifications

Use `@EnableScheduling` and `@Scheduled`.

---

# 26. Statistics

Periods: `CURRENT_MONTH`, `PREVIOUS_MONTH`, `YEAR`, `CUSTOM_RANGE`, `ALL_TIME`.

- [ ] Create Statistics Service
- [ ] Create General Statistics Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics`

Return fuel cost, maintenance cost, other expenses, total cost, and distance driven.

---

# 27. Fuel Statistics

- [ ] Create Fuel Statistics Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics/fuel`

Return total liters, total fuel cost, average km/L, average L/100km, cost per kilometer, and distance driven.

---

# 28. Maintenance Statistics

- [ ] Create Maintenance Statistics Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics/maintenance`

Return total maintenance cost, maintenance count, and cost by maintenance type.

---

# 29. Expense Statistics

- [ ] Create Expense Statistics Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics/expenses`

Return total expenses, expenses by category, and monthly expenses.

---

# 30. True Vehicle Cost

- [ ] Calculate Total Vehicle Cost
- [ ] Calculate Average Monthly Cost
- [ ] Calculate Cost Per Kilometer
- [ ] Create Total Cost Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics/total-cost`

```text
Total Vehicle Cost = Fuel + Maintenance + Expenses
```

Never count the same financial transaction twice.

---

# 31. Dashboard

- [ ] Create Vehicle Dashboard Endpoint — `GET /api/v1/vehicles/{vehicleId}/dashboard`

Return vehicle information, current mileage, monthly total cost, monthly fuel cost, average fuel efficiency, upcoming maintenance, upcoming reminders, and recent activity.

---

# 32. Recent Vehicle Activity

- [ ] **Create Activity Feed**

Combine fuel, maintenance, and expenses. Sort newest first.

---

# 33. JPA Auditing

- [ ] Enable JPA Auditing
- [ ] Create Base Auditable Entity

Use `@EnableJpaAuditing`, `@CreatedDate`, and `@LastModifiedDate`.

---

# 34. Soft Delete

- [ ] Implement Vehicle Soft Delete
- [ ] Exclude Deleted Vehicles

---

# 35. Database Indexes

- [ ] **Create Required Indexes**

Recommended:

```text
users.email
vehicles.user_id
fuel_records.vehicle_id
fuel_records.date
fuel_records.mileage
maintenance_records.vehicle_id
maintenance_records.date
expenses.vehicle_id
expenses.date
expenses.category
reminders.vehicle_id
reminders.target_date
reminders.target_mileage
devices.user_id
devices.fcm_token
```

---

# 36. Logging

- [ ] **Configure Logging**

Log request ID, HTTP method, path, response status, execution time, and unexpected exceptions.

Never log passwords, password hashes, access tokens, refresh tokens, authorization headers, Google secrets, Firebase private keys, FCM tokens, or database passwords.

---

# 37. Docker

- [ ] **Create Backend Dockerfile**
- [ ] **Create Docker Compose**
- [ ] **Configure SQL Server Container**
- [x] **Verify Local Docker Environment**
- [x] **Verify Testcontainers Docker Connectivity**

Verified locally with Docker Desktop, Docker Engine 29.x, Linux containers, Apple Silicon, and Testcontainers 1.21.3.

Verification commands:

```bash
docker --version
docker info
docker ps
```

Testcontainers successfully detected the Docker environment through `unix:///var/run/docker.sock`.

The local compatibility resource is:

```text
src/test/resources/docker-java.properties
```

```properties
api.version=1.44
```

Docker Compose must eventually include the Spring Boot API and Microsoft SQL Server.

Final verification:

```bash
docker compose up
```

---

# 38. Development Seed Data

- [ ] **Create Development Seed**

Create development-only demo user, vehicle, fuel records, maintenance records, expenses, and reminders.

Never create production seed credentials.

---

# 39. Testing

Testing stack:

```text
JUnit 5
Mockito
Spring Boot Test
MockMvc
Testcontainers
Microsoft SQL Server
```

Pure unit tests must not start database containers unless persistence is genuinely required.

Database-backed integration tests must use isolated Microsoft SQL Server Testcontainers. H2 must not be used.

## Authentication Tests

- [x] Register User
- [x] Duplicate Email
- [x] Login
- [x] Invalid Credentials
- [x] JWT Validation
- [x] Refresh Token
- [x] Refresh Rotation
- [x] Logout
- [x] Google Authentication

## Vehicle Tests

- [ ] Create Vehicle
- [ ] Get Vehicles
- [ ] Get Vehicle
- [ ] Update Vehicle
- [ ] Update Mileage
- [ ] Delete Vehicle
- [ ] Ownership Validation

## Fuel Tests

- [ ] Create Fuel Record
- [ ] Update Fuel Record
- [ ] Delete Fuel Record
- [ ] Fuel Efficiency
- [ ] L/100km
- [ ] Cost Per Kilometer

## Maintenance Tests

- [ ] Create Maintenance
- [ ] Update Maintenance
- [ ] Delete Maintenance

## Expense Tests

- [ ] Create Expense
- [ ] Update Expense
- [ ] Delete Expense

## Reminder Tests

- [ ] Create Reminder
- [ ] Complete Reminder
- [ ] Date Reminder
- [ ] Mileage Reminder
- [ ] Duplicate Notification Prevention

## Statistics Tests

- [ ] General Statistics
- [ ] Fuel Statistics
- [ ] Maintenance Statistics
- [ ] Expense Statistics
- [ ] True Vehicle Cost

## Security Tests

- [x] Unauthenticated Request
- [x] Invalid JWT
- [x] Expired JWT
- [ ] Cross-User Vehicle Access
- [ ] Cross-User Fuel Access
- [ ] Cross-User Maintenance Access
- [ ] Cross-User Expense Access
- [ ] Cross-User Reminder Access

---

# 40. Testcontainers

- [x] **Configure Microsoft SQL Server Testcontainer**
- [x] **Configure Spring Boot Testcontainers Integration**
- [x] **Remove H2 Test Database**
- [x] **Verify Docker Connectivity**
- [ ] **Verify Complete SQL Server Integration Test Suite**

Database-backed integration tests use Microsoft SQL Server 2022, Testcontainers, Docker Desktop, and Spring Boot service connections.

Required dependencies:

```text
org.springframework.boot:spring-boot-testcontainers
org.testcontainers:junit-jupiter
org.testcontainers:mssqlserver
```

SQL Server image:

```text
mcr.microsoft.com/mssql/server:2022-latest
```

Integration tests must never connect to development or production databases for destructive operations.

The Testcontainers database must be isolated and disposable.

Full verification requires:

```bash
./mvnw clean verify
```

to finish with all SQL Server-backed integration tests passing.

---

# 41. Final Build Verification

- [ ] **Maven Build Passes**
- [ ] **All Automated Tests Pass**
- [ ] **SQL Server Integration Tests Pass**
- [ ] **Application Starts Using Development Profile**
- [ ] **Application Starts Using Production Profile Configuration**
- [ ] **Docker Build Passes**
- [x] **Local Docker Environment Verified**
- [x] **Testcontainers Docker Connectivity Verified**

Verification command:

```bash
./mvnw clean verify
```

Successful verification requires compilation, unit tests, SQL Server Testcontainer startup, Flyway migrations, Hibernate schema validation, integration tests, and finally:

```text
BUILD SUCCESS
```

No production secrets may be committed.

The repository contains unit, context, security, and authentication integration tests covering response serialization, validation and explicit exception mapping, protected/public security routes, JWT validation, password encoding, registration, login, refresh-token rotation, logout, current-user access, and Google authentication behavior.

The test infrastructure has been migrated away from H2. Database-backed integration tests target Microsoft SQL Server through Testcontainers.

---

# V1 Main Feature Progress

- [x] Project Setup
- [ ] Microsoft SQL Server
- [ ] Flyway
- [ ] Common API Infrastructure
- [x] Global Error Handling
- [x] Spring Security
- [x] JWT Authentication
- [x] Registration
- [x] Email / Password Login
- [x] Refresh Tokens
- [x] Logout
- [x] Google Authentication
- [ ] User Profile
- [ ] Vehicle Management
- [ ] Vehicle Ownership Security
- [ ] Fuel Tracking
- [ ] Fuel Calculations
- [ ] Maintenance
- [ ] Expenses
- [ ] Reminders
- [ ] Device Management
- [ ] Firebase Push Notifications
- [ ] Reminder Scheduler
- [ ] Statistics
- [ ] True Vehicle Cost
- [ ] Dashboard
- [ ] Recent Activity
- [x] Swagger
- [ ] Testing
- [ ] Docker
- [ ] Production Verification

---

# Not Included in V1

Codex must not implement the following features unless the project owner explicitly changes the scope:

```text
AI Assistant
AI Insights
Receipt OCR
Receipt Scanner
PDF Reports
OBD-II Integration
Workshop Marketplace
Government Integration
Vehicle Sharing
Multiple Drivers
Payments
Subscriptions
Social Features
```

---

# Missing Configuration Rule

Whenever Codex encounters an implementation requiring project-owner input such as an API key, client ID, client secret, private key, Firebase credential, Google OAuth credential, certificate, production URL, external service account, or signing key, Codex must:

1. Continue implementing everything possible without the missing value.
2. Never invent credentials.
3. Never add fake production values.
4. Add the missing requirement to `SECRETS_SETUP.md`.
5. Document the exact name, purpose, required/optional status, environment variable name, where to obtain it, setup instructions, configuration location, dependencies, and verification method.
6. Leave the related feature unchecked until it is actually verified.

---

# Instructions for Codex

Before each task:

```text
1. Read README.md.
2. Read AGENTS.md.
3. Read SECRETS_SETUP.md.
4. Inspect the existing implementation.
5. Identify the next unfinished requested feature.
```

During implementation:

```text
Follow existing architecture.
Use Java 17.
Use Spring Boot.
Use Microsoft SQL Server.
Use Flyway.
Use Microsoft SQL Server Testcontainers for database-backed integration tests.
Do not introduce H2.
Do not start database containers for pure unit tests.
Use DTOs.
Use constructor injection.
Validate input.
Validate ownership.
Handle errors.
Add Swagger documentation.
Add appropriate tests.
Never hardcode secrets.
```

After implementation:

```text
Run relevant unit tests.
Run SQL Server integration tests where applicable.
Run the full Maven build when appropriate.
Fix failures.
Do not mark build verification complete without BUILD SUCCESS.
Update README checklist.
Update SECRETS_SETUP.md if new owner configuration is required.
Do not mark incomplete features complete.
```

---

# Goal

The V1 backend should be:

```text
Secure
Maintainable
Testable
Production-oriented
Easy to extend
Easy to integrate with Flutter
```

The priority is building a stable core product, not maximizing the number of features.
