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
- BCrypt password hashing.
- JWT access authentication.
- Hashed refresh-token storage and rotation.
- Logout and refresh-token revocation.
- Current-user resolution.
- Backend-verified Google ID-token authentication.
- User profile retrieval, update, and soft deletion.
- Microsoft SQL Server persistence.
- Flyway schema migrations.
- Hibernate schema validation.
- OpenAPI / Swagger documentation.
- Microsoft SQL Server Testcontainers for database-backed integration tests.

Google sessions use the same Sayarti JWT and refresh-token model as local sessions.

Microsoft SQL Server is the only relational database engine used by the project. H2 must not be introduced for integration testing.

Local verification has completed successfully with:

```text
./mvnw clean verify

BUILD SUCCESS
Tests run: 17
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

No Vehicle, Fuel, Maintenance, Expense, Reminder, Statistics, Dashboard, or other V1 vehicle-domain feature has been implemented yet.

Email verification for LOCAL accounts is now part of the V1 scope and must be implemented before Vehicle Management begins.

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
7. Change:

```md
- [ ] Feature
```

to:

```md
- [x] Feature
```

only after the Definition of Done is satisfied.

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

External integrations also require real configuration and verification before being marked complete where applicable.

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
- [ ] **Create / Finalize `.env.example`**

Current expected baseline:

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

EMAIL_OTP_EXPIRATION_SECONDS=300
EMAIL_OTP_RESEND_COOLDOWN_SECONDS=60
EMAIL_OTP_MAX_ATTEMPTS=5
```

When an email provider is selected, add its required environment-variable names without committing real values.

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
- [ ] **Complete V1 Database Schema**

Required tables:

- [x] `users`
- [x] `refresh_tokens`
- [ ] `email_verification_otps`
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
- [ ] **Create Sorting Support**
- [ ] **Create Date Range Filtering**

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

OTP-related error codes remain planned until Email Verification is implemented.

---

# 9. API Versioning

- [x] **Configure API Base Path**

All V1 APIs start with:

```text
/api/v1
```

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

- [x] **Register LOCAL User**

Endpoint:

```http
POST /api/v1/auth/register
```

Current registration validates:

- First name.
- Last name.
- Email.
- Unique email.
- Password.
- Password hashing.
- DTO validation.

After Email Verification is implemented, LOCAL registration becomes:

```text
Register
→ Create LOCAL user with emailVerified = false
→ Generate verification OTP
→ Send verification email
→ Do not issue normal authenticated application access yet
```

## Login

- [x] **Login With Email and Password**

Endpoint:

```http
POST /api/v1/auth/login
```

After Email Verification is implemented, unverified LOCAL accounts must be rejected with:

```text
AUTH_EMAIL_NOT_VERIFIED
```

## Refresh

- [x] **Refresh Session**

```http
POST /api/v1/auth/refresh
```

## Logout

- [x] **Logout**

```http
POST /api/v1/auth/logout
```

---

# 14. Email Verification for LOCAL Accounts

Email Verification is required for LOCAL email/password registration before Vehicle Management begins.

Google users whose Google ID token contains a verified email do not require Sayarti OTP verification.

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

- [ ] **Add Email Verification State to User**

Add:

```text
emailVerified
```

Rules:

```text
LOCAL registration → false
Verified Google identity → true
```

- [ ] **Create Email Verification OTP Entity and Table**

Recommended fields:

```text
id
userId
otpHash
expiresAt
attemptCount
verifiedAt
createdAt
```

Optional fields:

```text
invalidatedAt
lastAttemptAt
```

Requirements:

- Microsoft SQL Server.
- Flyway.
- UTC-aware timestamp strategy.
- Foreign key to user.
- Useful indexes.
- Never persist OTP plaintext.

- [ ] **Create OTP Generation Service**

Requirements:

- Cryptographically secure 6-digit numeric OTP.
- Hash before persistence.
- Never log OTP in production.
- Keep generation separate from delivery.

Recommended config:

```text
OTP Length: 6 digits
OTP Expiration: 5 minutes
Maximum Attempts: 5
Resend Cooldown: 60 seconds
```

Suggested properties:

```text
EMAIL_OTP_EXPIRATION_SECONDS
EMAIL_OTP_MAX_ATTEMPTS
EMAIL_OTP_RESEND_COOLDOWN_SECONDS
```

- [ ] **Create Email Delivery Abstraction**

Create an abstraction such as:

```text
EmailService
```

Authentication logic must not depend directly on a provider.

Possible providers later:

```text
SMTP
Resend
SendGrid
AWS SES
```

Do not select or hardcode a provider silently.

When a provider is implemented, update `SECRETS_SETUP.md` with exact credential instructions.

- [ ] **Modify LOCAL Registration to Start Email Verification**

After creating a LOCAL user:

1. Set `emailVerified = false`.
2. Generate OTP.
3. Store only OTP hash.
4. Send verification email.
5. Do not issue normal access/refresh tokens before successful verification.

- [ ] **Verify Email OTP**

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
→ Mark verification record used
→ Mark user emailVerified = true
→ Issue access token
→ Issue refresh token
→ Return AuthResponse
```

- [ ] **Resend Email Verification OTP**

Endpoint:

```http
POST /api/v1/auth/resend-verification
```

Requirements:

- Resend cooldown.
- Rate limiting.
- Invalidate previous active OTP.
- Generate a new OTP.
- Store only hash.
- Send new email.
- Avoid multiple valid OTPs.
- Avoid unnecessary account enumeration.

- [ ] **Block Unverified LOCAL Login**

If:

```text
emailVerified = false
```

then login must not issue tokens.

Return:

```text
AUTH_EMAIL_NOT_VERIFIED
```

- [ ] **Protect Refresh Flow for Unverified LOCAL Accounts**

Unverified LOCAL accounts must not gain access through refresh behavior.

- [ ] **Handle Verified Google Users**

Verified Google identities must have:

```text
emailVerified = true
```

and must not require Sayarti OTP.

- [ ] **Prevent OTP Abuse**

Implement:

- Attempt limit.
- Resend cooldown.
- Endpoint rate limiting.
- OTP expiration.
- Single active OTP policy.
- No plaintext persistence.
- No OTP logging.
- Safe errors.

- [ ] **Email Verification Swagger Documentation**

Document registration verification behavior, verify endpoint, resend endpoint, OTP errors, and resend/rate-limit behavior.

- [ ] **Email Verification Tests**

Required coverage:

- [ ] LOCAL registration creates unverified user.
- [ ] OTP is stored hashed.
- [ ] Valid OTP verifies email.
- [ ] Verification issues access and refresh tokens.
- [ ] Invalid OTP rejected.
- [ ] Expired OTP rejected.
- [ ] Used OTP rejected.
- [ ] Invalidated OTP rejected.
- [ ] Maximum attempts enforced.
- [ ] Resend creates a new OTP.
- [ ] Previous OTP invalidated.
- [ ] Resend cooldown enforced.
- [ ] Unverified LOCAL login blocked.
- [ ] Verified LOCAL login succeeds.
- [ ] Unverified refresh access blocked.
- [ ] Verified Google user bypasses Sayarti OTP.
- [ ] Database-backed tests use Microsoft SQL Server Testcontainers.
- [ ] Pure OTP unit tests do not start Testcontainers unless persistence is required.

Email Verification is complete only when:

```bash
./mvnw clean verify
```

returns:

```text
BUILD SUCCESS
```

and the selected email provider has been verified where provider integration is part of the task.

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

The backend verifies:

- Google signature.
- Issuer.
- `GOOGLE_CLIENT_ID`.
- Expiration.
- Subject.
- Verified email.

Automatic linking to an existing LOCAL account with the same verified email is forbidden.

Return:

```text
409 AUTH_ACCOUNT_LINKING_REQUIRED
```

No `GOOGLE_CLIENT_SECRET` is required for the current ID-token verification flow.

---

# 16. User Profile

- [x] **Create User Entity**
- [x] **Get Current User**
- [x] **Update Current User**
- [x] **Delete Account**

Endpoints:

```http
GET /api/v1/users/me
PATCH /api/v1/users/me
DELETE /api/v1/users/me
```

Only safe profile fields may be edited.

Account deletion revokes active refresh tokens and prevents future authentication.

---

# 17. Vehicle Management

- [ ] Create Vehicle Entity
- [ ] Create Vehicle Repository
- [ ] Create Vehicle DTOs
- [ ] Create Vehicle — `POST /api/v1/vehicles`
- [ ] Get User Vehicles — `GET /api/v1/vehicles`
- [ ] Get Vehicle Details — `GET /api/v1/vehicles/{vehicleId}`
- [ ] Update Vehicle — `PATCH /api/v1/vehicles/{vehicleId}`
- [ ] Update Mileage — `PATCH /api/v1/vehicles/{vehicleId}/mileage`
- [ ] Delete Vehicle — `DELETE /api/v1/vehicles/{vehicleId}`

Use soft delete.

---

# 18. Vehicle Ownership Security

- [ ] Create Vehicle Ownership Validation
- [ ] Protect Vehicle APIs
- [ ] Protect Fuel APIs
- [ ] Protect Maintenance APIs
- [ ] Protect Expense APIs
- [ ] Protect Reminder APIs
- [ ] Protect Statistics APIs
- [ ] Protect Dashboard APIs

---

# 19. Fuel Tracking

- [ ] Create Fuel Entity
- [ ] Create Fuel Repository
- [ ] Create Fuel DTOs
- [ ] Create Fuel Record
- [ ] Get Fuel History
- [ ] Get Fuel Record
- [ ] Update Fuel Record
- [ ] Delete Fuel Record

---

# 20. Fuel Calculations

- [ ] Calculate Distance Between Refills
- [ ] Calculate Fuel Efficiency
- [ ] Calculate L/100km
- [ ] Calculate Cost Per Kilometer
- [ ] Calculate Monthly Fuel Cost
- [ ] Calculate Total Fuel Cost
- [ ] Calculate Average Fuel Efficiency

---

# 21. Maintenance

- [ ] Create Maintenance Entity
- [ ] Create Maintenance Repository
- [ ] Create Maintenance DTOs
- [ ] Create Maintenance Record
- [ ] Get Maintenance History
- [ ] Get Maintenance Record
- [ ] Update Maintenance Record
- [ ] Delete Maintenance Record

---

# 22. Expenses

- [ ] Create Expense Entity
- [ ] Create Expense Repository
- [ ] Create Expense DTOs
- [ ] Create Expense
- [ ] Get Expenses
- [ ] Get Expense
- [ ] Update Expense
- [ ] Delete Expense

---

# 23. Reminders

- [ ] Create Reminder Entity
- [ ] Create Reminder Repository
- [ ] Create Reminder DTOs
- [ ] Create Reminder
- [ ] Get Reminders
- [ ] Update Reminder
- [ ] Complete Reminder
- [ ] Delete Reminder

---

# 24. Device Management

- [ ] Create Device Entity
- [ ] Create Device Repository
- [ ] Create Device DTOs
- [ ] Register Device
- [ ] Update FCM Token
- [ ] Delete Device

---

# 25. Firebase Cloud Messaging

- [ ] Configure Firebase Admin SDK
- [ ] Create Notification Service
- [ ] Send Notification To Device
- [ ] Send Notification To User Devices
- [ ] License Expiration Notification
- [ ] Insurance Expiration Notification
- [ ] Maintenance Notification
- [ ] Mileage Reminder Notification
- [ ] Custom Reminder Notification

---

# 26. Reminder Scheduler

- [ ] Enable Scheduling
- [ ] Create Reminder Scheduler
- [ ] Check Date-Based Reminders
- [ ] Check Mileage-Based Reminders
- [ ] Prevent Duplicate Notifications

---

# 27. Statistics

- [ ] Create Statistics Service
- [ ] Create General Statistics Endpoint

---

# 28. Fuel Statistics

- [ ] Create Fuel Statistics Endpoint

---

# 29. Maintenance Statistics

- [ ] Create Maintenance Statistics Endpoint

---

# 30. Expense Statistics

- [ ] Create Expense Statistics Endpoint

---

# 31. True Vehicle Cost

- [ ] Calculate Total Vehicle Cost
- [ ] Calculate Average Monthly Cost
- [ ] Calculate Cost Per Kilometer
- [ ] Create Total Cost Endpoint

---

# 32. Dashboard

- [ ] Create Vehicle Dashboard Endpoint

---

# 33. Recent Vehicle Activity

- [ ] Create Activity Feed

---

# 34. JPA Auditing

- [ ] Enable JPA Auditing
- [ ] Create Base Auditable Entity

---

# 35. Soft Delete

- [ ] Implement Vehicle Soft Delete
- [ ] Exclude Deleted Vehicles

---

# 36. Database Indexes

- [ ] Complete Required V1 Indexes

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

---

# 37. Logging

- [ ] Configure Application Request Logging

Never log passwords, password hashes, OTP values, OTP hashes, JWTs, refresh tokens, Google tokens, Firebase keys, FCM tokens, email-provider credentials, or DB passwords.

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

## Email Verification Tests

- [ ] Registration Creates Unverified LOCAL User
- [ ] OTP Stored Hashed
- [ ] Valid OTP
- [ ] Invalid OTP
- [ ] Expired OTP
- [ ] Used OTP
- [ ] Maximum Attempts
- [ ] Resend OTP
- [ ] Previous OTP Invalidated
- [ ] Resend Cooldown
- [ ] Unverified LOCAL Login Blocked
- [ ] Verified LOCAL Login
- [ ] Unverified Refresh Access Blocked
- [ ] Verified Google Account Bypasses OTP

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

## Vehicle Tests

- [ ] Create Vehicle
- [ ] Get Vehicles
- [ ] Get Vehicle
- [ ] Update Vehicle
- [ ] Update Mileage
- [ ] Reject Mileage Decrease
- [ ] Delete Vehicle
- [ ] Deleted Vehicle Excluded
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

# 41. Testcontainers

- [x] Configure Microsoft SQL Server Testcontainer
- [x] Configure Spring Boot Testcontainers Integration
- [x] Remove H2 Test Database
- [x] Verify Docker Connectivity
- [x] Verify Existing SQL Server Integration Test Suite

Current verified baseline:

```text
BUILD SUCCESS
Tests run: 17
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
- [ ] Email Verification
- [x] Email / Password Login Base Flow
- [x] Refresh Tokens
- [x] Logout
- [x] Google Authentication
- [x] User Profile
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
6. Document:
   - Exact name.
   - Purpose.
   - Required/optional status.
   - Environment-variable name.
   - Where to obtain it.
   - Exact setup steps.
   - Where to configure it.
   - What depends on it.
   - How to verify it.
7. Leave external-integration verification unchecked until it has actually been verified.

For Email Verification specifically, once the email provider is selected, `SECRETS_SETUP.md` must document exactly where the project owner obtains the required email credentials and where they are configured.

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
Keep code readable and properly indented.
Do not generate compressed or minified-looking code.
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
Do not mark external integration complete without real verification where applicable.
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

Rules:

```text
Use 4 spaces for indentation.
Do not use tabs for indentation.
Keep one statement per line.
Use spaces around operators and after commas.
Use braces consistently.
Separate logical method sections where useful.
Avoid excessively long lines.
Break long method calls and argument lists into readable multiline blocks.
Organize imports.
Remove unused imports.
Do not compress methods merely to reduce line count.
Avoid unrelated formatting-only diffs.
```
