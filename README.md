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

The Java 17 Maven/Spring Boot foundation now includes local email/password
authentication, hashed passwords, JWT access authentication, rotating hashed
refresh tokens, logout, current-user resolution, and backend-verified Google ID-token
authentication. Google sessions use the same JWT and refresh-token rotation as local
sessions. The authentication schema is
managed by Flyway and its HTTP contract is documented with OpenAPI. No vehicle or
other V1 business feature has been implemented yet.

Checklist items below remain unchecked where runtime verification still depends on
downloading Maven artifacts, owner-provided configuration, SQL Server, or Docker.

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

---

# Core Development Rules

Codex must follow these rules during development:

1. Read this entire README before implementing features.
2. Read `AGENTS.md` before making code changes.
3. Read `SECRETS_SETUP.md` before configuring external integrations.
4. Work one logical feature at a time.
5. Do not implement features outside the V1 scope.
6. Do not mark partially implemented features as complete.
7. Change:

```md
- [ ] Feature
```

to:

```md
- [x] Feature
```

only after the Definition of Done is satisfied.

8. Every external key, secret, credential, certificate, client ID, token, or configuration value that cannot be generated locally must be documented in:

```text
SECRETS_SETUP.md
```

9. Never commit actual secrets to Git.
10. Never hardcode secrets in Java source files.

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
Tests
+
Build Verification
```

If any required part is missing, the feature stays unchecked.

---

# 1. Initial Project Setup

- [ ] **Initialize Spring Boot Project**

Create the Spring Boot project using:

```text
Java 17
Maven
```

Base package:

```text
com.sayarti.backend
```

Main class:

```text
SayartiApplication
```

- [ ] **Add Core Dependencies**

Required dependencies:

```text
Spring Web
Spring Security
Spring Data JPA
Validation
Microsoft SQL Server JDBC Driver
Flyway
Lombok
OpenAPI / Swagger
JWT
OAuth2 Client
Firebase Admin SDK
Spring Boot Test
Testcontainers
```

- [ ] **Configure Maven**

Configure:

```text
Java 17
UTF-8
Spring Boot Maven Plugin
```

The following command must succeed:

```bash
./mvnw clean verify
```

---

# 2. Application Configuration

- [ ] **Create Application Configuration**

Create:

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
src/test/resources/application-test.yml
```

- [ ] **Configure Environment Variables**

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

Any variable requiring input from the project owner must be documented in:

```text
SECRETS_SETUP.md
```

- [ ] **Create `.env.example`**

Create a safe example file containing variable names only.

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

Copy `.env.example` to an ignored `.env` for Docker Compose, or export the same
variables in the shell when running the application directly. Spring Boot does not
read `.env` files by itself.

---

# 3. Microsoft SQL Server

- [ ] **Configure SQL Server Connection**

Use Microsoft SQL Server as the primary database.

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

- [ ] **Add Microsoft SQL Server JDBC Driver**

Use:

```text
com.microsoft.sqlserver:mssql-jdbc
```

- [ ] **Configure Hibernate**

Use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Never use `ddl-auto=update` as the database migration strategy.

---

# 4. Flyway Database Migrations

- [ ] **Configure Flyway**

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

- [ ] `users`
- [ ] `refresh_tokens`
- [ ] `vehicles`
- [ ] `fuel_records`
- [ ] `maintenance_records`
- [ ] `expenses`
- [ ] `reminders`
- [ ] `devices`

All schema changes must be performed through Flyway migrations.

---

# 5. Project Architecture

- [ ] **Create Feature-Based Architecture**

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

- [ ] **Create Standard Success Response**
- [ ] **Create Standard Error Response**
- [ ] **Create Pagination Response**
- [ ] **Create Sorting Support**
- [ ] **Create Date Range Filtering**

Success example:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

Error example:

```json
{
  "success": false,
  "error": {
    "code": "VEHICLE_NOT_FOUND",
    "message": "Vehicle not found"
  }
}
```

---

# 8. Global Error Handling

- [ ] **Create Global Exception Handler**

Use `@RestControllerAdvice`.

Handle:

```text
Validation Errors
Authentication Errors
Authorization Errors
Resource Not Found
Business Validation Errors
Database Errors
Unexpected Errors
```

- [ ] **Create Error Code Enum**

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

- [ ] **Configure API Base Path**

All V1 APIs must start with:

```text
/api/v1
```

---

# 10. Swagger / OpenAPI

- [ ] **Configure Swagger**
- [ ] **Document All APIs**

Swagger UI should be available in development at:

```text
/swagger-ui/index.html
```

Each API must document description, authentication, parameters, body, success responses, and error responses.

---

# 11. Security Configuration

- [ ] **Configure Spring Security**
- [ ] **Configure Stateless Authentication**
- [ ] **Configure Public Routes**
- [ ] **Configure Password Encoder**
- [ ] **Configure CORS**
- [ ] **Configure Security Headers**

Use:

```text
SecurityFilterChain
SessionCreationPolicy.STATELESS
```

Public routes:

```text
/api/v1/auth/**
/swagger-ui/**
/v3/api-docs/**
```

---

# 12. JWT Authentication

- [ ] **Create JWT Service**
- [ ] **Create JWT Authentication Filter**
- [ ] **Configure Access Token Expiration**
- [ ] **Configure Refresh Token Expiration**
- [ ] **Create Refresh Token Storage**
- [ ] **Implement Refresh Token Rotation**

JWT service must support:

```text
Generate Access Token
Validate Access Token
Parse User Identity
Generate Refresh Token
Validate Refresh Token
```

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

- [ ] **Register User**

```http
POST /api/v1/auth/register
```

Requirements:

```text
First Name
Last Name
Valid Email
Unique Email
Password Validation
Password Hashing
DTO Validation
```

- [ ] **Login With Email and Password**

```http
POST /api/v1/auth/login
```

Response:

```text
Access Token
Refresh Token
User Information
```

- [ ] **Refresh Session**

```http
POST /api/v1/auth/refresh
```

- [ ] **Logout**

```http
POST /api/v1/auth/logout
```

Refresh token must be invalidated.

---

# 14. Google OAuth 2.0

- [ ] **Configure Google OAuth 2.0**
- [ ] **Implement Google Login Flow**
- [ ] **Verify Google Identity Token**
- [ ] **Handle Existing Users / Account Linking**

Support Google authentication suitable for Android and iOS mobile applications.

The backend must verify Google-issued identity tokens. Never trust profile data sent by the mobile client without verifying the token.

Any required Google credential must be documented in `SECRETS_SETUP.md`.

### Implemented mobile flow

The mobile application obtains a Google ID token using the `openid`, `email`, and
`profile` scopes and sends only that token to `POST /api/v1/auth/google` as
`{"idToken":"<google-id-token>"}`. The backend verifies Google's signature plus the
Google issuer, `GOOGLE_CLIENT_ID` audience, and expiration, and requires a subject and
verified email. Names and email are read exclusively from verified token claims. A
new Google account is created on first login; later logins resolve the stored Google
subject and issue the normal Sayarti access/refresh token response. Google ID tokens
are neither logged nor stored.

Automatic linking to an existing `LOCAL` account with the same verified email is
intentionally forbidden. The endpoint returns `409 AUTH_ACCOUNT_LINKING_REQUIRED`;
the user must first authenticate to that local account before any future explicit
linking flow can safely be offered. This release does not implement such a linking
endpoint and never changes a local account based only on possession of a Google token.

No `GOOGLE_CLIENT_SECRET` is used or required because the backend does not exchange
an authorization code.

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

Providers:

```text
LOCAL
GOOGLE
```

- [ ] **Create User Entity**
- [ ] **Get Current User**

```http
GET /api/v1/users/me
```

- [ ] **Update Current User**

```http
PATCH /api/v1/users/me
```

- [ ] **Delete Account**

```http
DELETE /api/v1/users/me
```

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

- [ ] **Create Vehicle Entity**
- [ ] **Create Vehicle Repository**
- [ ] **Create Vehicle DTOs**
- [ ] **Create Vehicle**

```http
POST /api/v1/vehicles
```

- [ ] **Get User Vehicles**

```http
GET /api/v1/vehicles
```

- [ ] **Get Vehicle Details**

```http
GET /api/v1/vehicles/{vehicleId}
```

- [ ] **Update Vehicle**

```http
PATCH /api/v1/vehicles/{vehicleId}
```

- [ ] **Update Mileage**

```http
PATCH /api/v1/vehicles/{vehicleId}/mileage
```

Mileage must not normally decrease.

- [ ] **Delete Vehicle**

```http
DELETE /api/v1/vehicles/{vehicleId}
```

Use soft delete.

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

- [ ] **Create Fuel Entity**
- [ ] **Create Fuel Repository**
- [ ] **Create Fuel DTOs**
- [ ] **Create Fuel Record**

```http
POST /api/v1/vehicles/{vehicleId}/fuel
```

- [ ] **Get Fuel History**

```http
GET /api/v1/vehicles/{vehicleId}/fuel
```

Support pagination, sorting, and date filtering.

- [ ] **Get Fuel Record**

```http
GET /api/v1/vehicles/{vehicleId}/fuel/{fuelId}
```

- [ ] **Update Fuel Record**

```http
PATCH /api/v1/vehicles/{vehicleId}/fuel/{fuelId}
```

- [ ] **Delete Fuel Record**

```http
DELETE /api/v1/vehicles/{vehicleId}/fuel/{fuelId}
```

---

# 19. Fuel Calculations

- [ ] **Calculate Distance Between Refills**
- [ ] **Calculate Fuel Efficiency**
- [ ] **Calculate L/100km**
- [ ] **Calculate Cost Per Kilometer**
- [ ] **Calculate Monthly Fuel Cost**
- [ ] **Calculate Total Fuel Cost**
- [ ] **Calculate Average Fuel Efficiency**

Formulas:

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

Fields:

```text
id
vehicleId
type
mileage
cost
date
workshopName
notes
nextMaintenanceMileage
nextMaintenanceDate
createdAt
updatedAt
```

- [ ] Create Maintenance Entity
- [ ] Create Maintenance Repository
- [ ] Create Maintenance DTOs
- [ ] **Create Maintenance Record**

```http
POST /api/v1/vehicles/{vehicleId}/maintenance
```

- [ ] **Get Maintenance History**

```http
GET /api/v1/vehicles/{vehicleId}/maintenance
```

Support pagination, date filtering, type filtering, and sorting.

- [ ] **Get Maintenance Record**

```http
GET /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}
```

- [ ] **Update Maintenance Record**

```http
PATCH /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}
```

- [ ] **Delete Maintenance Record**

```http
DELETE /api/v1/vehicles/{vehicleId}/maintenance/{maintenanceId}
```

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

Fields:

```text
id
vehicleId
category
amount
date
description
createdAt
updatedAt
```

- [ ] Create Expense Entity
- [ ] Create Expense Repository
- [ ] Create Expense DTOs
- [ ] **Create Expense**

```http
POST /api/v1/vehicles/{vehicleId}/expenses
```

- [ ] **Get Expenses**

```http
GET /api/v1/vehicles/{vehicleId}/expenses
```

Support pagination, category filtering, date filtering, and sorting.

- [ ] **Get Expense**

```http
GET /api/v1/vehicles/{vehicleId}/expenses/{expenseId}
```

- [ ] **Update Expense**

```http
PATCH /api/v1/vehicles/{vehicleId}/expenses/{expenseId}
```

- [ ] **Delete Expense**

```http
DELETE /api/v1/vehicles/{vehicleId}/expenses/{expenseId}
```

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

Fields:

```text
id
vehicleId
type
title
description
targetDate
targetMileage
notificationEnabled
completed
lastNotificationSentAt
createdAt
updatedAt
```

- [ ] Create Reminder Entity
- [ ] Create Reminder Repository
- [ ] Create Reminder DTOs
- [ ] **Create Reminder**

```http
POST /api/v1/vehicles/{vehicleId}/reminders
```

Support date-based, mileage-based, and combined reminders.

- [ ] **Get Reminders**

```http
GET /api/v1/vehicles/{vehicleId}/reminders
```

- [ ] **Update Reminder**

```http
PATCH /api/v1/vehicles/{vehicleId}/reminders/{reminderId}
```

- [ ] **Complete Reminder**

```http
PATCH /api/v1/vehicles/{vehicleId}/reminders/{reminderId}/complete
```

- [ ] **Delete Reminder**

```http
DELETE /api/v1/vehicles/{vehicleId}/reminders/{reminderId}
```

---

# 23. Device Management

Fields:

```text
id
userId
deviceId
fcmToken
platform
createdAt
updatedAt
```

Platforms:

```text
ANDROID
IOS
```

- [ ] Create Device Entity
- [ ] Create Device Repository
- [ ] Create Device DTOs
- [ ] **Register Device**

```http
POST /api/v1/devices
```

- [ ] **Update FCM Token**
- [ ] **Delete Device**

---

# 24. Firebase Cloud Messaging

- [ ] **Configure Firebase Admin SDK**
- [ ] **Create Notification Service**
- [ ] **Send Notification To Device**
- [ ] **Send Notification To User Devices**
- [ ] **License Expiration Notification**
- [ ] **Insurance Expiration Notification**
- [ ] **Maintenance Notification**
- [ ] **Mileage Reminder Notification**
- [ ] **Custom Reminder Notification**

Any required Firebase credential must be documented in `SECRETS_SETUP.md`.

---

# 25. Reminder Scheduler

- [ ] **Enable Scheduling**
- [ ] **Create Reminder Scheduler**
- [ ] **Check Date-Based Reminders**
- [ ] **Check Mileage-Based Reminders**
- [ ] **Prevent Duplicate Notifications**

Use `@EnableScheduling` and `@Scheduled`.

Mileage reminders must also be evaluated when vehicle mileage changes.

---

# 26. Statistics

Periods:

```text
CURRENT_MONTH
PREVIOUS_MONTH
YEAR
CUSTOM_RANGE
ALL_TIME
```

- [ ] **Create Statistics Service**
- [ ] **Create General Statistics Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/statistics
```

Return:

```text
Fuel Cost
Maintenance Cost
Other Expenses
Total Cost
Distance Driven
```

Use optimized database queries where appropriate.

---

# 27. Fuel Statistics

- [ ] **Create Fuel Statistics Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/statistics/fuel
```

Return:

```text
Total Liters
Total Fuel Cost
Average km/L
Average L/100km
Cost Per Kilometer
Distance Driven
```

---

# 28. Maintenance Statistics

- [ ] **Create Maintenance Statistics Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/statistics/maintenance
```

Return:

```text
Total Maintenance Cost
Maintenance Count
Cost By Maintenance Type
```

---

# 29. Expense Statistics

- [ ] **Create Expense Statistics Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/statistics/expenses
```

Return:

```text
Total Expenses
Expenses By Category
Monthly Expenses
```

---

# 30. True Vehicle Cost

- [ ] **Calculate Total Vehicle Cost**
- [ ] **Calculate Average Monthly Cost**
- [ ] **Calculate Cost Per Kilometer**
- [ ] **Create Total Cost Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/statistics/total-cost
```

Formula:

```text
Total Vehicle Cost = Fuel + Maintenance + Expenses
```

Never count the same financial transaction twice.

---

# 31. Dashboard

- [ ] **Create Vehicle Dashboard Endpoint**

```http
GET /api/v1/vehicles/{vehicleId}/dashboard
```

Return:

```text
Vehicle Information
Current Mileage
Monthly Total Cost
Monthly Fuel Cost
Average Fuel Efficiency
Upcoming Maintenance
Upcoming Reminders
Recent Activity
```

The endpoint should reduce unnecessary mobile API calls.

---

# 32. Recent Vehicle Activity

- [ ] **Create Activity Feed**

Combine:

```text
Fuel
Maintenance
Expenses
```

Sort newest first.

---

# 33. JPA Auditing

- [ ] **Enable JPA Auditing**
- [ ] **Create Base Auditable Entity**

Use:

```text
@EnableJpaAuditing
@CreatedDate
@LastModifiedDate
```

Common fields:

```text
createdAt
updatedAt
```

---

# 34. Soft Delete

- [ ] **Implement Vehicle Soft Delete**
- [ ] **Exclude Deleted Vehicles**

Soft-deleted vehicles must not appear in normal APIs.

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

Log:

```text
Request ID
HTTP Method
Path
Response Status
Execution Time
Unexpected Exceptions
```

Never log:

```text
Passwords
Password Hashes
Access Tokens
Refresh Tokens
Authorization Headers
Google Client Secrets
Firebase Private Keys
FCM Tokens
Database Passwords
```

---

# 37. Docker

- [ ] **Create Backend Dockerfile**
- [ ] **Create Docker Compose**
- [ ] **Configure SQL Server Container**
- [ ] **Verify Docker Environment**

Docker Compose must include:

```text
Spring Boot API
Microsoft SQL Server
```

Verification:

```bash
docker compose up
```

---

# 38. Development Seed Data

- [ ] **Create Development Seed**

Create development-only data:

```text
Demo User
Demo Vehicle
Fuel Records
Maintenance Records
Expenses
Reminders
```

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
```

## Authentication Tests

- [ ] Register User
- [ ] Duplicate Email
- [ ] Login
- [ ] Invalid Credentials
- [ ] JWT Validation
- [ ] Refresh Token
- [ ] Refresh Rotation
- [ ] Logout
- [ ] Google Authentication

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

- [ ] Unauthenticated Request
- [ ] Invalid JWT
- [ ] Expired JWT
- [ ] Cross-User Vehicle Access
- [ ] Cross-User Fuel Access
- [ ] Cross-User Maintenance Access
- [ ] Cross-User Expense Access
- [ ] Cross-User Reminder Access

---

# 40. Testcontainers

- [ ] **Configure Microsoft SQL Server Testcontainer**

Integration tests must use Microsoft SQL Server Testcontainers where database-specific behavior matters.

Do not rely exclusively on H2.

---

# 41. Final Build Verification

- [ ] **Maven Build Passes**
- [ ] **All Automated Tests Pass**
- [ ] **Application Starts Using Development Profile**
- [ ] **Application Starts Using Production Profile Configuration**
- [ ] **Docker Build Passes**

Verification command:

```bash
./mvnw clean verify
```

No production secrets may be committed.

The repository currently contains unit/context tests for response serialization,
validation and explicit exception mapping, protected/public security routes,
security headers, and password encoding. SQL Server Testcontainers are available
as a dependency for future database-specific integration tests; no database schema
exists yet by design.

---

# V1 Main Feature Progress

- [ ] Project Setup
- [ ] Microsoft SQL Server
- [ ] Flyway
- [ ] Common API Infrastructure
- [ ] Global Error Handling
- [ ] Spring Security
- [ ] JWT Authentication
- [ ] Registration
- [ ] Email / Password Login
- [ ] Refresh Tokens
- [ ] Logout
- [ ] Google Authentication
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
- [ ] Swagger
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

Whenever Codex encounters an implementation requiring project-owner input such as:

```text
API Key
Client ID
Client Secret
Private Key
Firebase Credential
Google OAuth Credential
Certificate
Production URL
External Service Account
Signing Key
```

Codex must:

1. Continue implementing everything possible without the missing value.
2. Never invent credentials.
3. Never add fake production values.
4. Add the missing requirement to `SECRETS_SETUP.md`.
5. Document:
   - Exact name.
   - Purpose.
   - Required/optional status.
   - Environment variable name.
   - Where to obtain it.
   - Step-by-step instructions.
   - Where to configure it.
   - What depends on it.
   - How to verify it.
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
Run relevant tests.
Run build when appropriate.
Fix failures.
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
