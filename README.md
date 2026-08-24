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

The Java 17 Maven/Spring Boot foundation currently includes:

- Local email/password registration and login.
- Implemented and locally verified LOCAL email verification, including hashed OTP storage, verification, resend controls, and authentication guards for unverified accounts.
- Spring Mail SMTP adapter implemented with mock-based automated coverage; real SMTP delivery awaits owner credentials and inbox verification.
- BCrypt password hashing.
- JWT access authentication.
- Hashed refresh-token storage and rotation.
- Logout and refresh-token revocation.
- Current-user resolution.
- Backend-verified Google ID-token authentication.
- User profile retrieval, update, and soft deletion.
- Country and currency reference data, country selection, and user default-currency support.
- Vehicle creation, retrieval, update, mileage updates, and soft deletion.
- Vehicle-specific ownership enforcement for every vehicle endpoint.
- Fuel-record creation, history and detail retrieval, update, and soft deletion.
- Vehicle ownership enforcement for every fuel-record endpoint.
- Maintenance-record creation, history and detail retrieval, update, and soft deletion.
- Vehicle ownership enforcement for every maintenance-record endpoint.
- Expense creation, history and detail retrieval, update, and soft deletion.
- Vehicle ownership enforcement for every expense endpoint.
- Reminder creation, retrieval, update, completion, and soft deletion.
- Vehicle ownership enforcement for every reminder endpoint.
- Authenticated device registration, FCM-token updates, and device deletion.
- User ownership enforcement for every device endpoint.
- Provider-neutral notification delivery, Firebase Admin SDK configuration, single-device and
  user-device fan-out, invalid-token cleanup, and safe unavailable-provider fallback, all with
  automated local coverage. Real-device Firebase delivery still requires owner credentials and
  external verification.
- Scheduled date- and mileage-based reminder processing with persisted successful-delivery state,
  row locking, retry behavior, and duplicate-delivery prevention.
- General vehicle statistics with vehicle ownership enforcement.
- Fuel statistics with vehicle ownership enforcement.
- Fuel distance, efficiency (km/L and L/100km), per-currency cost-per-kilometer,
  monthly cost, total cost, and average-efficiency calculations.
- Maintenance statistics with vehicle ownership enforcement.
- Expense statistics with vehicle ownership enforcement.
- True Vehicle Cost statistics with vehicle ownership enforcement.
- Vehicle dashboard summaries with vehicle ownership enforcement.
- Recent vehicle activity with vehicle ownership enforcement.
- Allowlisted, deterministic sorting for vehicle, fuel-record, maintenance-record, and expense
  list endpoints.
- Inclusive business-date range filtering for fuel-record, maintenance-record, and expense
  histories.
- Microsoft SQL Server persistence.
- Flyway schema migrations.
- V1 database indexes aligned with the current verified repository and scheduler query patterns.
- Hibernate schema validation.
- OpenAPI / Swagger documentation.
- Safe application request logging with request correlation IDs.
- Microsoft SQL Server Testcontainers for database-backed integration tests.

Google sessions use the same Sayarti JWT and refresh-token model as local sessions.

Microsoft SQL Server is the only relational database engine used by the project. H2 must not be introduced for integration testing.

Local verification has completed successfully with:

```text
./mvnw clean verify

BUILD SUCCESS
Tests run: 157
Failures: 0
Errors: 0
Skipped: 0
```

The verified test flow includes:

```text
Docker / Testcontainers
→ Microsoft SQL Server 2022
→ Flyway migrations
→ Hibernate schema validation
→ Spring context
→ automated tests
→ BUILD SUCCESS
```

Country/Currency Foundation, Vehicle Management, Vehicle-specific ownership protection,
Fuel Tracking CRUD with ownership protection, Maintenance CRUD with ownership protection,
Expense CRUD with ownership protection, Reminder CRUD and completion with ownership
protection, Device Management, General Vehicle Statistics, Fuel Statistics, Maintenance
Statistics, Expense Statistics, True Vehicle Cost, and the Vehicle Dashboard are implemented
with ownership protection and locally verified. Recent Vehicle Activity is also implemented with
ownership protection and locally verified. Fuel Calculations, the Reminder Scheduler, and
Database Indexes are implemented and locally verified. Energy Tracking is not implemented.

LOCAL email verification is implemented and locally verified. The provider-neutral SMTP
adapter is implemented and automated-test covered. Real SMTP delivery remains
unverified until owner credentials are configured and an OTP is received, as documented
in `SECRETS_SETUP.md`.

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
- Email delivery abstraction
- Firebase Admin SDK
- Docker
- JUnit 5
- Mockito
- Testcontainers

Microsoft SQL Server is used for application persistence and database-backed integration testing.

H2 must not be introduced as an integration-test database.

---

# Core Development Rules

Codex must follow these rules during development:

1. Read this entire README before implementing features.
2. Read `AGENTS.md` before making code changes if the file exists.
3. Read `SECRETS_SETUP.md` before configuring external integrations.
4. Work one logical feature at a time.
5. Do not implement features outside the V1 scope.
6. Do not mark partially implemented features as complete.
7. Change `- [ ] Feature` to `- [x] Feature` only after the Definition of Done is satisfied.
8. Every external key, secret, credential, certificate, client ID, token, email-provider credential, or configuration value that cannot be generated locally must be documented in `SECRETS_SETUP.md`.
9. Never commit actual secrets to Git.
10. Never hardcode secrets in Java source files.
11. Microsoft SQL Server is the only supported relational database engine.
12. Do not introduce H2 for integration testing.
13. Database-backed integration tests must use Microsoft SQL Server Testcontainers.
14. Pure unit tests must not start Docker/Testcontainers.
15. Never point automated tests at development or production databases.
16. Flyway is the source of truth for database schema changes.
17. Keep `spring.jpa.hibernate.ddl-auto=validate`.
18. Do not use `ddl-auto=update`.
19. Use UTC-aware persisted timestamps consistently with the project's SQL Server mapping.
20. Do not log passwords, OTP values, JWTs, refresh tokens, Google tokens, Firebase credentials, email-provider secrets, or database credentials.

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
Microsoft SQL Server Integration Tests where applicable
+
Build Verification
```

If any required part is missing, the feature stays unchecked.

---

# 1. Initial Project Setup

- [x] **Initialize Spring Boot Project**
- [x] **Add Core Dependencies**
- [x] **Configure Maven**

Verified with:

```bash
./mvnw clean verify
```

Result:

```text
BUILD SUCCESS
```

---

# 2. Application Configuration

- [x] **Create Application Configuration**
- [x] **Configure Environment Variables**
- [x] **Create / Finalize `.env.example`**

`.env.example` now reflects the environment variables used by the current V1 backend for
database and JWT configuration, the Google client ID, the Firebase service-account path,
email OTP policy, SMTP, CORS, and reminder scheduler settings. It contains only safe
placeholders and defaults; the real `.env` remains owner-managed, protected, and must not be
overwritten or committed.

---

# 3. Microsoft SQL Server

- [x] **Configure SQL Server Connection**
- [x] **Add Microsoft SQL Server JDBC Driver**
- [x] **Configure Hibernate**

Use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Database-backed integration tests use isolated Microsoft SQL Server instances through Testcontainers.

---

# 4. Flyway Database Migrations

- [x] **Configure Flyway**
- [x] **Complete V1 Database Schema**

Required tables:

- [x] `users`
- [x] `refresh_tokens`
- [x] `email_verification_otps`
- [x] `vehicles`
- [x] `currencies`
- [x] `countries`
- [x] `fuel_records`
- [x] `maintenance_records`
- [x] `expenses`
- [x] `reminders`
- [x] `devices`

All schema changes must be performed through Flyway migrations. The complete required V1 table
set is validated by Hibernate and the Microsoft SQL Server Testcontainers integration suite in the
verified 157-test build.

---

# 5. Project Architecture

- [x] **Create Feature-Based Architecture**

Target structure includes:

```text
auth/
user/
email/
vehicle/
fuel/
maintenance/
expense/
reminder/
statistics/
notification/
device/
security/
common/
config/
```

---

# 6. Architecture Rules

- [x] Controllers contain HTTP-related logic only.
- [x] Business logic belongs in services.
- [x] Database access goes through repositories.
- [x] JPA entities are never returned directly from controllers.
- [x] Request DTOs are used for client input.
- [x] Response DTOs are used for client output.
- [x] Constructor injection is used.
- [x] Field injection is not used.
- [x] Business exceptions are explicit and meaningful.
- [ ] Shared logic remains free from unnecessary duplication as the project expands.

---

# 7. Common API Response

- [x] **Create Standard Success Response**
- [x] **Create Standard Error Response**
- [x] **Create Pagination Response**
- [x] **Create Sorting Support**
- [x] **Create Date Range Filtering**

Sorting is available on the following paginated `GET` endpoints through optional `sortBy` and
`sortDirection` query parameters:

- `/api/v1/vehicles`: `createdAt`, `brand`, `model`, `year`, or `currentMileage`.
- `/api/v1/vehicles/{vehicleId}/fuel-records`: `filledAt`, `createdAt`, `odometerKm`, or
  `totalCost`.
- `/api/v1/vehicles/{vehicleId}/maintenance-records`: `serviceDate`, `createdAt`, `mileageKm`,
  or `cost`.
- `/api/v1/vehicles/{vehicleId}/expenses`: `expenseDate`, `createdAt`, `amount`, or `title`.

`sortDirection` accepts `asc` or `desc` case-insensitively. With no sorting parameters, vehicles
default to `createdAt` descending; fuel, maintenance, and expense histories default respectively
to `filledAt`, `serviceDate`, and `expenseDate` descending, with `createdAt` and then `id` as
deterministic tie-breakers. Explicit sorts use `id` as their deterministic tie-breaker.
Unsupported fields and directions return `400 VALIDATION_ERROR` rather than exposing arbitrary
entity fields.

Date-range filtering is available on the fuel-record, maintenance-record, and expense history
endpoints above through optional `from` and `to` query parameters. Both parameters are ISO-8601
UTC-aware instants and are inclusive; they filter `filledAt`, `serviceDate`, and `expenseDate`,
respectively. Either bound may be supplied independently. Malformed instants and ranges where
`from` is later than `to` return `400 VALIDATION_ERROR`; equal bounds are valid.

Sorting and date filters are applied before the existing `page`/`size` pagination, so response
metadata describes the filtered result. All queries retain their existing authenticated ownership
constraints and exclude soft-deleted vehicles or records.

---

# 8. Global Error Handling

- [x] **Create Global Exception Handler**
- [x] **Create Error Code Enum**

Current and planned codes include:

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
AUTH_ACCOUNT_LINKING_REQUIRED

AUTH_EMAIL_NOT_VERIFIED
AUTH_EMAIL_ALREADY_VERIFIED
AUTH_OTP_INVALID
AUTH_OTP_EXPIRED
AUTH_OTP_ATTEMPTS_EXCEEDED
AUTH_OTP_RESEND_TOO_SOON

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

OTP-related codes are implemented as part of LOCAL Email Verification.

---

# 9. API Versioning

- [x] **Configure API Base Path**

All V1 APIs start with `/api/v1`.

---

# 10. Swagger / OpenAPI

- [x] **Configure Swagger**
- [x] **Document Existing APIs**

Swagger UI:

```text
/swagger-ui/index.html
```

---

# 11. Security Configuration

- [x] **Configure Spring Security**
- [x] **Configure Stateless Authentication**
- [x] **Configure Public Routes**
- [x] **Configure Password Encoder**
- [x] **Configure CORS**
- [x] **Configure Security Headers**

---

# 12. JWT Authentication

- [x] **Create JWT Service**
- [x] **Create JWT Authentication Filter**
- [x] **Configure Access Token Expiration**
- [x] **Configure Refresh Token Expiration**
- [x] **Create Refresh Token Storage**
- [x] **Implement Refresh Token Rotation**

---

# 13. User Authentication

## Registration

- [x] **Register LOCAL User** — `POST /api/v1/auth/register`

Current registration validates first name, last name, unique email, password, password hashing, and DTO validation.

LOCAL registration now follows this flow:

```text
Register
→ Create LOCAL user with emailVerified = false
→ Generate verification OTP
→ Send verification email
→ Do not issue normal authenticated application access yet
```

## Login

- [x] **Login With Email and Password** — `POST /api/v1/auth/login`

Unverified LOCAL accounts return `AUTH_EMAIL_NOT_VERIFIED` and do not receive access or refresh tokens.

## Refresh

- [x] **Refresh Session** — `POST /api/v1/auth/refresh`

## Logout

- [x] **Logout** — `POST /api/v1/auth/logout`

---

# 14. Email Verification for LOCAL Accounts

Email Verification for LOCAL email/password registration is implemented and locally verified.

Verified Google users do not require Sayarti OTP verification.

## Target Registration Flow

```text
POST /api/v1/auth/register
        ↓
Create LOCAL user
emailVerified = false
        ↓
Generate secure 6-digit OTP
        ↓
Store OTP hash
        ↓
Send OTP to registered email
        ↓
POST /api/v1/auth/verify-email
        ↓
Verify OTP
        ↓
emailVerified = true
        ↓
Issue Sayarti access + refresh tokens
```

- [x] **Add Email Verification State to User**

Rules:

```text
LOCAL registration → false
Verified Google identity → true
```

- [x] **Create Email Verification OTP Entity and Table**

Recommended fields:

```text
id
userId
otpHash
expiresAt
attemptCount
verifiedAt
createdAt
invalidatedAt
```

Requirements:

- Microsoft SQL Server.
- Flyway.
- UTC-aware timestamp strategy.
- Foreign key to user.
- Useful indexes.
- Never persist OTP plaintext.

- [x] **Create OTP Generation Service**

Requirements:

- Cryptographically secure 6-digit numeric OTP.
- Hash before persistence.
- Never log OTP in production.
- Default expiration: 5 minutes.
- Maximum attempts: 5.
- Resend cooldown: 60 seconds.

Suggested configuration:

```text
EMAIL_OTP_EXPIRATION_SECONDS=300
EMAIL_OTP_MAX_ATTEMPTS=5
EMAIL_OTP_RESEND_COOLDOWN_SECONDS=60
```

- [x] **Create Email Delivery Abstraction**

Create an abstraction such as `EmailService`. Authentication logic must not depend directly on SMTP, Resend, SendGrid, SES, or another provider.

Any required provider credentials must be documented in `SECRETS_SETUP.md`.

The provider-neutral abstraction and Spring Mail SMTP implementation are complete.
Authentication remains independent of SMTP details. Real SMTP delivery is not yet
verified and remains an owner action in `SECRETS_SETUP.md`.

- [x] **Modify LOCAL Registration to Start Email Verification**

After creating a LOCAL user:

1. Set `emailVerified = false`.
2. Generate OTP.
3. Store only OTP hash.
4. Send verification email.
5. Do not issue normal access/refresh tokens before verification.

- [x] **Verify Email OTP**

Endpoint:

```http
POST /api/v1/auth/verify-email
```

Example:

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

Successful flow:

```text
Find eligible LOCAL user
→ Find active verification OTP
→ Check expiration
→ Check attempt limit
→ Verify OTP hash
→ Mark OTP used
→ Mark user emailVerified = true
→ Issue access token
→ Issue refresh token
→ Return AuthResponse
```

- [x] **Resend Email Verification OTP**

Endpoint:

```http
POST /api/v1/auth/resend-verification
```

Requirements:

- Resend cooldown.
- Server-side rate limiting.
- Previous OTP invalidation.
- Single-active-OTP policy.
- No account-enumeration leakage where avoidable.

- [x] **Block Unverified LOCAL Login**
- [x] **Protect Refresh Flow for Unverified LOCAL Accounts**
- [x] **Handle Verified Google Users**
- [x] **Prevent OTP Abuse**
- [x] **Email Verification Swagger Documentation**

- [x] **Email Verification Tests**

Required coverage:

- [x] LOCAL registration creates unverified user.
- [x] OTP is stored hashed.
- [x] Valid OTP verifies email.
- [x] Verification issues access and refresh tokens.
- [x] Invalid OTP rejected.
- [x] Expired OTP rejected.
- [x] Used OTP rejected.
- [x] Invalidated OTP rejected.
- [x] Maximum attempts enforced.
- [x] Resend creates a new OTP.
- [x] Previous OTP invalidated.
- [x] Resend cooldown enforced.
- [x] Unverified LOCAL login blocked.
- [x] Verified LOCAL login succeeds.
- [x] Unverified refresh access blocked.
- [x] Verified Google user bypasses Sayarti OTP.
- [x] Database-backed tests use Microsoft SQL Server Testcontainers.
- [x] Pure OTP unit tests do not start Testcontainers unless persistence is required.

The LOCAL Email Verification backend flow is verified. The SMTP adapter has mock-based
automated tests and does not make network calls during the test suite. This does not
claim real email delivery: credentials and inbox-based end-to-end verification remain
pending in `SECRETS_SETUP.md`.

---

# 15. Google OAuth 2.0

- [x] **Configure Google OAuth 2.0**
- [x] **Implement Google Login Flow**
- [x] **Verify Google Identity Token**
- [x] **Handle Existing Users / Account Linking**

Endpoint:

```http
POST /api/v1/auth/google
```

The backend verifies Google signature, issuer, `GOOGLE_CLIENT_ID`, expiration, subject, and verified email.

Automatic linking to an existing LOCAL account with the same verified email is forbidden and returns `409 AUTH_ACCOUNT_LINKING_REQUIRED`.

No `GOOGLE_CLIENT_SECRET` is required for the current ID-token verification flow.

---

# 16. User Profile

- [x] **Create User Entity**
- [x] **Get Current User** — `GET /api/v1/users/me`
- [x] **Update Current User** — `PATCH /api/v1/users/me`
- [x] **Delete Account** — `DELETE /api/v1/users/me`

Profile updates accept only `firstName` and `lastName`.

Account deletion uses the existing soft-delete strategy, revokes active refresh tokens, and prevents the deleted account from authenticating.

---

# 17. Vehicle Management

- [x] Create Vehicle Entity
- [x] Create Vehicle Repository
- [x] Create Vehicle DTOs
- [x] Create Vehicle — `POST /api/v1/vehicles`
- [x] Get User Vehicles — `GET /api/v1/vehicles`
- [x] Get Vehicle Details — `GET /api/v1/vehicles/{vehicleId}`
- [x] Update Vehicle — `PATCH /api/v1/vehicles/{vehicleId}`
- [x] Update Mileage — `PATCH /api/v1/vehicles/{vehicleId}/mileage`
- [x] Delete Vehicle — `DELETE /api/v1/vehicles/{vehicleId}`

Use soft delete.

Vehicle powertrain, fuel-type, fuel-tank, battery-capacity, and estimated-range metadata are
part of Vehicle Management. Energy Tracking is a future feature and is not implemented by
these vehicle fields.

---

# 18. Vehicle Ownership Security

- [x] Create Vehicle Ownership Validation
- [x] Protect Vehicle APIs
- [x] Protect Fuel APIs
- [x] Protect Maintenance APIs
- [x] Protect Expense APIs
- [x] Protect Reminder APIs
- [x] Protect Statistics APIs
- [x] Protect Dashboard APIs

---

# 19. Fuel Tracking

- [x] Create Fuel Entity
- [x] Create Fuel Repository
- [x] Create Fuel DTOs
- [x] Create Fuel Record
- [x] Get Fuel History
- [x] Get Fuel Record
- [x] Update Fuel Record
- [x] Delete Fuel Record

---

# 20. Fuel Calculations

- [x] Calculate Distance Between Refills
- [x] Calculate Fuel Efficiency
- [x] Calculate L/100km
- [x] Calculate Cost Per Kilometer
- [x] Calculate Monthly Fuel Cost
- [x] Calculate Total Fuel Cost
- [x] Calculate Average Fuel Efficiency

Fuel calculations use eligible successive odometer distances. Aggregate efficiency is reported in
km/L and L/100km; monetary totals, monthly cost, and cost per kilometer remain separated by
currency. Unit and SQL Server integration tests cover normal, empty, insufficient-history,
soft-delete, ordering, and multi-currency behavior.

---

# 21. Maintenance

- [x] Create Maintenance Entity
- [x] Create Maintenance Repository
- [x] Create Maintenance DTOs
- [x] Create Maintenance Record
- [x] Get Maintenance History
- [x] Get Maintenance Record
- [x] Update Maintenance Record
- [x] Delete Maintenance Record

---

# 22. Expenses

- [x] Create Expense Entity
- [x] Create Expense Repository
- [x] Create Expense DTOs
- [x] Create Expense
- [x] Get Expenses
- [x] Get Expense
- [x] Update Expense
- [x] Delete Expense

---

# 23. Reminders

- [x] Create Reminder Entity
- [x] Create Reminder Repository
- [x] Create Reminder DTOs
- [x] Create Reminder
- [x] Get Reminders
- [x] Update Reminder
- [x] Complete Reminder
- [x] Delete Reminder

---

# 24. Device Management

- [x] Create Device Entity
- [x] Create Device Repository
- [x] Create Device DTOs
- [x] Register Device
- [x] Update FCM Token
- [x] Delete Device

---

# 25. Firebase Cloud Messaging

- [x] Configure Firebase Admin SDK
- [x] Create Notification Service
- [ ] Send Notification To Device
- [ ] Send Notification To User Devices
- [ ] License Expiration Notification
- [ ] Insurance Expiration Notification
- [ ] Maintenance Notification
- [ ] Mileage Reminder Notification
- [ ] Custom Reminder Notification

The Firebase infrastructure is implemented and locally automated-test covered: valid service
account configuration creates the Firebase Admin components, the provider-neutral notification
service supports one device and all registered devices for a user, permanently invalid tokens are
removed, and an unavailable-provider fallback fails safely when Firebase is not configured.
Provider tests construct Firebase messages without network access. The delivery-related items
above remain incomplete because no repository evidence verifies delivery to a real Firebase
project/device. License- and insurance-expiration notification completion is also not established.

---

# 26. Reminder Scheduler

- [x] Enable Scheduling
- [x] Create Reminder Scheduler
- [x] Check Date-Based Reminders
- [x] Check Mileage-Based Reminders
- [x] Prevent Duplicate Notifications

The scheduler polls due active reminders in bounded batches. The processor evaluates date and
vehicle-mileage triggers, sends through the provider-neutral notification service, and records
`notificationDeliveredAt` only after at least one successful device delivery. A pessimistic row
lock plus persisted delivery state prevents duplicate processing; unsuccessful delivery remains
retryable. Automated tests cover both trigger types, exclusions, successful and partial delivery,
retry behavior, and repeated-run duplicate prevention.

---

# 27. Statistics

- [x] Create Statistics Service
- [x] Create General Statistics Endpoint

---

# 28. Fuel Statistics

- [x] Create Fuel Statistics Endpoint

---

# 29. Maintenance Statistics

- [x] Create Maintenance Statistics Endpoint — `GET /api/v1/vehicles/{vehicleId}/statistics/maintenance`

This authenticated endpoint aggregates active maintenance records for an owned, active vehicle.
Missing, deleted, and cross-user vehicles return `VEHICLE_NOT_FOUND` to hide resource existence,
and soft-deleted maintenance records are excluded. Monetary calculations use `BigDecimal`; total
and average maintenance costs are grouped by their original currency without conversion. The
response includes the total maintenance-record count, latest maintenance date and mileage, and a
deterministic maintenance-category aggregation. An empty history returns a zero record count,
empty currency and category arrays, and `null` latest date and mileage.

---

# 30. Expense Statistics

- [x] Create Expense Statistics Endpoint

---

# 31. True Vehicle Cost

- [x] Calculate Total Vehicle Cost
- [x] Calculate Average Monthly Cost
- [x] Calculate Cost Per Kilometer
- [x] Create Total Cost Endpoint

---

# 32. Dashboard

- [x] Create Vehicle Dashboard Endpoint

---

# 33. Recent Vehicle Activity

- [x] Create Activity Feed — `GET /api/v1/vehicles/{vehicleId}/activity`

This authenticated endpoint returns a bounded, newest-first timeline for an owned, active vehicle.
It combines active fuel, maintenance, and expense records with completed reminders, uses each
source record's business event timestamp, retains original monetary values and currencies, and
excludes soft-deleted records and incomplete reminders. The optional `limit` parameter defaults to
20 and accepts values from 1 through 100. Missing, deleted, and cross-user vehicles return
`VEHICLE_NOT_FOUND` to preserve resource hiding.

---

# 34. JPA Auditing

- [x] Enable JPA Auditing
- [x] Create Base Auditable Entity

Spring Data JPA auditing is enabled. The shared `BaseAuditableEntity` manages `createdAt` and
`updatedAt` as `Instant` timestamps while remaining compatible with the existing SQL Server
`datetimeoffset` columns. `createdAt` remains unchanged after entity creation, while `updatedAt`
is updated automatically whenever an entity is modified. `JpaAuditingIntegrationTest` verifies
this behavior using Microsoft SQL Server Testcontainers.

---

# 35. Soft Delete

- [x] Implement Vehicle Soft Delete
- [x] Exclude Deleted Vehicles

---

# 36. Database Indexes

- [x] Complete Required V1 Indexes

Recommended areas:

```text
users.email
users.google_subject
refresh_tokens.user_id
email_verification_otps.user_id
email_verification_otps.expires_at
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

The verified V1 query-pattern indexes are provided by
`V15__add_v1_query_pattern_indexes.sql` and covered by `DatabaseIndexesIntegrationTest`.
They align with the current V1 repository and query patterns for vehicle ownership and active
records, active fuel records ordered by date, active maintenance records ordered by date,
active/completed reminder access, and reminder-scheduler scans for pending `DATE` and `MILEAGE`
triggers. The scheduler indexes use filtered SQL Server predicates so pending-reminder scans stay
narrow. These indexes align with the current verified query patterns; no benchmarked performance
gain is claimed.

Existing unique constraints and indexes, including those for users, refresh tokens, email
verification OTPs, and devices, were retained and were not duplicated by the V1 query-pattern
migration. Their continued presence is also verified by `DatabaseIndexesIntegrationTest`.

---

# 37. Logging

- [x] Configure Application Request Logging

Completed HTTP requests are logged with the request method, request path, response status, request
duration, authenticated user ID when available, and an `X-Request-ID` correlation/request ID. A
valid incoming request ID is reused and returned in the response; an invalid or missing value is
safely replaced with a generated UUID. Logging deliberately excludes query strings, request
bodies, and headers so sensitive data is not captured.

Sensitive values must never be logged, including passwords, password hashes, OTP values or hashes,
JWT/access tokens, refresh tokens, `Authorization` headers, `Cookie` or `Set-Cookie` values, Google
tokens, Firebase credentials, FCM tokens, email-provider credentials, and database credentials.

---

# 38. Docker

- [ ] Create / Finalize Backend Dockerfile
- [ ] Create / Finalize Docker Compose
- [ ] Configure SQL Server Container
- [x] Verify Local Docker Environment
- [x] Verify Testcontainers Docker Connectivity

Compatibility resource:

```text
src/test/resources/docker-java.properties
```

```properties
api.version=1.44
```

---

# 39. Development Seed Data

- [ ] Create Development Seed

---

# 40. Testing

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

## User Profile Tests

- [x] Get Current User
- [x] Unauthenticated Profile Access
- [x] Update First Name
- [x] Update Last Name
- [x] Profile Validation
- [x] Protected Fields Not Writable
- [x] Delete Account
- [x] Refresh Tokens Revoked After Deletion
- [x] Deleted User Cannot Authenticate

## Email Verification Tests

- [x] Registration Creates Unverified LOCAL User
- [x] OTP Stored Hashed
- [x] Valid OTP
- [x] Invalid OTP
- [x] Expired OTP
- [x] Used OTP
- [x] Maximum Attempts
- [x] Resend OTP
- [x] Previous OTP Invalidated
- [x] Resend Cooldown
- [x] Unverified LOCAL Login Blocked
- [x] Verified LOCAL Login
- [x] Unverified Refresh Access Blocked
- [x] Verified Google Account Bypasses OTP

## Vehicle Tests

- [x] Create Vehicle
- [x] Get Vehicles
- [x] Get Vehicle
- [x] Update Vehicle
- [x] Update Mileage
- [x] Reject Mileage Decrease
- [x] Delete Vehicle
- [x] Deleted Vehicle Excluded
- [x] Ownership Validation

## Fuel Tests

- [x] Create Fuel Record
- [x] Update Fuel Record
- [x] Delete Fuel Record
- [x] Fuel Efficiency
- [x] L/100km
- [x] Cost Per Kilometer

## Maintenance Tests

- [x] Create Maintenance
- [x] Update Maintenance
- [x] Delete Maintenance

## Expense Tests

- [x] Create Expense
- [x] Update Expense
- [x] Delete Expense

## Reminder Tests

- [x] Create Reminder
- [x] Complete Reminder
- [x] Date Reminder
- [x] Mileage Reminder
- [x] Duplicate Notification Prevention

## Statistics Tests

- [x] General Statistics
- [x] Fuel Statistics
- [x] Maintenance Statistics
- [x] Expense Statistics
- [x] True Vehicle Cost

## Dashboard Tests

- [x] Vehicle Dashboard

## Recent Vehicle Activity Tests

- [x] Recent Vehicle Activity Feed
- [x] Empty Vehicle Activity
- [x] Unified Newest-First Ordering
- [x] Soft-Deleted and Incomplete Activity Exclusion
- [x] Default and Custom Activity Limits

## Security Tests

- [x] Unauthenticated Request
- [x] Invalid JWT
- [x] Expired JWT
- [x] Cross-User Vehicle Access
- [x] Cross-User Fuel Access
- [x] Cross-User Maintenance Access
- [x] Cross-User Expense Access
- [x] Cross-User Reminder Access
- [x] Cross-User Recent Vehicle Activity Access

---

# 41. Testcontainers

- [x] Configure Microsoft SQL Server Testcontainer
- [x] Configure Spring Boot Testcontainers Integration
- [x] Remove H2 Test Database
- [x] Verify Docker Connectivity
- [x] Verify Existing SQL Server Integration Test Suite

Current verified baseline:

```text
BUILD SUCCESS
Tests run: 157
Failures: 0
Errors: 0
Skipped: 0
```

Future DB-backed feature tests must continue using the shared SQL Server Testcontainers setup.

---

# 42. Final Build Verification

- [x] Current Maven Build Passes
- [x] Current Automated Tests Pass
- [x] Current SQL Server Integration Tests Pass
- [ ] Application Starts Using Development Profile Against Development SQL Server
- [ ] Application Starts Using Production Profile Configuration
- [ ] Docker Compose Stack Verified
- [ ] Production Verification

Every new feature must rerun:

```bash
./mvnw clean verify
```

before its own checklist items are marked complete.

---

# V1 Main Feature Progress

- [x] Project Setup
- [x] Microsoft SQL Server
- [x] Flyway Infrastructure
- [x] Common API Infrastructure
- [x] Global Error Handling
- [x] Spring Security
- [x] JWT Authentication
- [x] Registration Base Flow
- [x] Email Verification
- [x] Email / Password Login Base Flow
- [x] Refresh Tokens
- [x] Logout
- [x] Google Authentication
- [x] User Profile
- [x] Vehicle Management
- [x] Vehicle Ownership Security
- [x] Fuel Tracking
- [x] Fuel Calculations
- [x] Maintenance
- [x] Expenses
- [x] Reminders
- [x] Device Management
- [ ] Firebase Push Notifications
- [x] Reminder Scheduler
- [x] Database Indexes
- [x] Statistics
- [x] Fuel Statistics
- [x] True Vehicle Cost
- [x] Dashboard
- [x] Recent Activity
- [x] Swagger
- [ ] Full V1 Testing
- [ ] Docker Compose
- [ ] Production Verification

---

# Not Included in V1

Codex must not implement the following unless the project owner explicitly changes scope:

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
Explicit LOCAL ↔ Google Account Linking UI/API
```

---

# Missing Configuration Rule

Whenever Codex encounters implementation requiring project-owner input such as an API key, client ID, client secret, private key, Firebase credential, Google OAuth credential, email-provider credential, SMTP credential, certificate, production URL, external service account, or signing key, Codex must:

1. Continue implementing everything possible without the missing value.
2. Never invent credentials.
3. Never add fake production values.
4. Never hardcode real secrets.
5. Add the missing requirement to `SECRETS_SETUP.md`.
6. Document the exact name, purpose, required/optional status, environment variable name, where to obtain it, setup steps, configuration location, dependencies, and verification method.
7. Leave external-integration verification unchecked until it has actually been verified.

---

# Instructions for Codex

Before each task:

```text
1. Read README.md.
2. Read AGENTS.md if present.
3. Read SECRETS_SETUP.md.
4. Inspect the existing implementation.
5. Identify the exact requested unfinished feature.
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
Use the existing UTC timestamp strategy.
Keep Java and SQL Server types consistent.
Use consistent standard Java formatting.
Organize imports.
Remove unused imports.
Avoid unrelated changes.
```

After implementation:

```text
Run relevant unit tests.
Run SQL Server integration tests where applicable.
Run ./mvnw clean verify.
Fix all failures.
Do not mark build verification complete without BUILD SUCCESS.
Update README.md only for truly completed items.
Update SECRETS_SETUP.md if new owner configuration is required.
Do not start the next feature.
Review the final diff for correctness and unnecessary changes.
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

## Code Formatting and Readability

Codex must write clean, consistently formatted, production-readable Java.

```text
Use 4 spaces for indentation.
Do not use tabs for indentation.
Keep one statement per line.
Use spaces around operators and after commas.
Use braces consistently.
Avoid excessively long lines.
Break long method calls and argument lists into readable multiline blocks.
Organize imports.
Remove unused imports.
Do not compress methods merely to reduce line count.
Avoid unrelated formatting-only diffs.
```

## Environment File Protection Rule

Codex must NEVER delete, blank, replace, reset, sanitize, or overwrite existing values in any local `.env` file.

The local `.env` file may contain real development credentials and project-owner configuration.

Rules:

1. Treat `.env` as owner-managed local configuration.
2. Preserve every existing key and value exactly as found.
3. Never replace an existing value with an empty value.
4. Never replace an existing value with an example or placeholder.
5. Never regenerate the entire `.env` file.
6. Never copy `.env.example` over `.env`.
7. Never remove unknown or unused keys from `.env`.
8. Never reorder or reformat `.env` unless explicitly requested by the project owner.
9. If a new environment variable is required:
    - append only the missing key to `.env` if editing `.env` is explicitly required,
    - do not modify any existing lines or values,
    - add the key to `.env.example` with a safe placeholder/default,
    - document the key in `SECRETS_SETUP.md` when owner input is required.
10. If a required key already exists in `.env`, keep its current value unchanged.
11. Never expose or copy real `.env` values into:
- README.md
- SECRETS_SETUP.md
- `.env.example`
- source code
- tests
- logs
- commits
12. `.env.example` may be updated freely with safe placeholders, but `.env` must be treated as protected data.

If there is any uncertainty about whether an existing `.env` value should be changed, leave it unchanged.

Violation of this rule is considered a destructive configuration change.
