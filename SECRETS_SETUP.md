# Sayarti Backend - Secrets & External Configuration Setup

This file documents every credential, secret, API key, certificate, client ID, client secret, external account, and owner-provided configuration required by the backend.

Actual secrets must **never** be committed to Git.

---

# Rules for Codex

Whenever implementation requires a value that is not available in the repository, Codex must add or update a section in this file.

Codex must never:

- Invent credentials.
- Hardcode real secrets.
- Commit real secrets.
- Put backend secrets inside the Flutter application.
- Mark an external integration as complete before it can be verified.

For every missing value, document:

```text
Name
Status
Required For
Environment Variable
Why It Is Needed
Where To Get It
Step-by-Step Setup
Where To Put It
Backend Usage
Verification Steps
```

Use checklist status:

```text
- [ ] Required from project owner
- [x] Provided and verified
```

---

# Environment Variables

Expected variables may include:

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
DB_TRUST_SERVER_CERTIFICATE=true

DEV_SEED_PASSWORD=

JWT_ACCESS_SECRET=
JWT_REFRESH_SECRET=
JWT_ACCESS_EXPIRATION=
JWT_REFRESH_EXPIRATION=

GOOGLE_CLIENT_ID=

FIREBASE_SERVICE_ACCOUNT_PATH=

EMAIL_OTP_EXPIRATION_SECONDS=300
EMAIL_OTP_RESEND_COOLDOWN_SECONDS=60
EMAIL_OTP_MAX_ATTEMPTS=5

SMTP_HOST=localhost
SMTP_PORT=25
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=no-reply@example.com
SMTP_TLS=false
SMTP_STARTTLS_REQUIRED=false
SMTP_SSL=false
SMTP_CONNECTION_TIMEOUT=10000
SMTP_TIMEOUT=60000
SMTP_WRITE_TIMEOUT=60000

ALLOWED_ORIGINS=

REMINDER_SCHEDULER_INTERVAL_MS=60000
REMINDER_SCHEDULER_BATCH_SIZE=100
```

Never commit a real `.env` file.

---

# Development Seed Password

## Status

- [ ] Configure locally when development seed users are wanted

## Required For

Logging in as the two development-only seed accounts created when, and only when, the Spring
`dev` profile is active. It is not required in production or automated tests.

## Environment Variable

```env
DEV_SEED_PASSWORD=
```

## Why It Is Needed

The backend uses its configured `PasswordEncoder` to hash this value for newly created seed
users. If it is missing or blank, user and related-data seeding is skipped with a safe log
message. The value and its hash are never logged.

## Where To Put It

Set it in the local ignored `.env` file or export it in the development process environment,
then activate the `dev` Spring profile. Never commit the value, and **never use this development
password as a production credential or reuse a production password here**.

## Backend Usage

`application-dev.yml` maps the environment variable to the development-only seed initializer.
The initializer is excluded from every profile other than `dev`.

## Verification

1. Start the backend with the `dev` profile and a nonblank `DEV_SEED_PASSWORD`.
2. Confirm the safe initialization message appears without credential data.
3. Log in to either documented development account using the configured local value.
4. Start without the `dev` profile and confirm no development records are created.

---

# 1. Microsoft SQL Server

## Status

- [ ] SQL Server credentials provided

## Required For

- Application database connection
- Flyway migrations
- Spring Data JPA / Hibernate
- Local and production startup

## Environment Variables

```env
DB_HOST=localhost
DB_PORT=1433
DB_NAME=sayarti
DB_USERNAME=
DB_PASSWORD=
```

## Where To Get It

For local development, SQL Server can run through Docker using Microsoft's official SQL Server container.

For production, credentials are obtained from the selected hosting/database provider.

## Step-by-Step Local Setup

1. Install Docker Engine with the Docker Compose plugin.
2. Choose a strong local SQL Server `sa` password that satisfies Microsoft's SQL
   Server password policy (upper case, lower case, number, and symbol).
3. Copy `.env.example` to `.env`.
4. Set `DB_USERNAME=sa` and put the chosen local password in `DB_PASSWORD`.
5. Keep `DB_HOST=localhost` when the API runs on the host. Docker Compose supplies
   `DB_HOST=database` to the API container automatically.
6. Run `docker compose up --build`. The Compose file starts SQL Server, creates the
   database named by `DB_NAME` when it does not exist, and then starts the API.

## Where To Put It

Use environment variables or the deployment platform's secrets manager.

Spring Boot configuration must reference these variables from `application.yml` or profile-specific configuration.

`DB_TRUST_SERVER_CERTIFICATE=true` is suitable for the local Docker certificate.
Set it according to the production provider's TLS instructions; production should
use a verifiable server certificate rather than weakening certificate validation.

## Verification

Verify that:

- The application connects successfully.
- Flyway migrations run.
- Hibernate schema validation succeeds.

---

# 2. JWT Access Secret

## Status

- [ ] JWT access secret generated

## Environment Variable

```env
JWT_ACCESS_SECRET=
```

## Required For

- Access token generation
- Access token validation

## How To Generate

Generate a strong random value:

```bash
openssl rand -base64 64
```

## Where To Put It

Set the generated value as:

```env
JWT_ACCESS_SECRET=<generated-value>
```

Never hardcode it in Java.

## Verification

- Login generates an access token.
- Authenticated APIs accept a valid token.
- Modified tokens are rejected.
- Expired tokens are rejected.

---

# 3. JWT Refresh Secret

## Status

- [ ] JWT refresh secret generated

## Environment Variable

```env
JWT_REFRESH_SECRET=
```

## Required For

- Refresh token generation
- Refresh token validation
- Session renewal

## How To Generate

Generate a separate secret:

```bash
openssl rand -base64 64
```

Do not reuse `JWT_ACCESS_SECRET`.

## Verification

- Login creates a refresh token.
- Refresh endpoint generates a new access/refresh pair.
- Invalid refresh tokens fail.
- Rotated refresh tokens cannot be reused.

---

# 4. JWT Expiration

## Status

- [ ] JWT expiration values confirmed

## Environment Variables

```env
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000
```

Recommended initial values:

```text
Access Token: 15 minutes
Refresh Token: 30 days
```

These can be changed later without changing application code.

---

# 5. Google OAuth 2.0

## Status

- [ ] Google Cloud project selected or created
- [ ] OAuth consent configuration completed
- [ ] Backend Google client ID provided
- [ ] Android OAuth client configured
- [ ] iOS OAuth client configured
- [ ] Google authentication verified end-to-end

## Required For

- Continue with Google
- Google registration
- Google login
- Google identity verification

---

# 6. Google Cloud Project

## Where To Get It

Open Google Cloud Console and create or select the project used for Sayarti.

Recommended project name:

```text
Sayarti
```

## Setup

Configure the OAuth / Google Auth Platform settings for the application.

For basic authentication, request only minimal scopes:

```text
openid
email
profile
```

Do not request unrelated Google permissions.

---

# 7. Google Backend Client ID

## Status

- [ ] Required from project owner

## Environment Variable

```env
GOOGLE_CLIENT_ID=
```

## Required For

Backend verification of Google identity tokens.

## Where To Get It

In Google Cloud Console:

```text
Google Auth Platform
→ Clients
→ Create OAuth Client
```

For Sayarti's implemented mobile ID-token flow, create a Web Application OAuth
client. Its client ID is the backend audience/server client ID that Android and iOS
request when obtaining the Google ID token.

## What To Copy

Copy the generated OAuth **Client ID**.

## Where To Put It

```env
GOOGLE_CLIENT_ID=<client-id>
```

## Backend Usage

The backend loads this as `sayarti.google.client-id` and accepts it as the sole token
audience. Startup fails when it is absent or blank, preventing accidental token
verification without audience validation.

## Verification

The backend must verify:

- Token signature.
- Issuer.
- Expiration.
- Audience.
- Verified user identity.

A token intended for another application must not be accepted when audience validation requires the Sayarti client ID.

---

# 8. Google Client Secret (Not Required)

## Status

- [x] Confirmed not required for the implemented flow

## Important

A Google client secret is **not automatically required** for every mobile Google Sign-In implementation.

If Flutter obtains a Google ID token and Spring Boot only verifies that token, the backend may not need a client secret.

If the backend performs an OAuth authorization-code exchange, a client secret may be required.

Sayarti implements the ID-token verification flow: Flutter obtains an ID token and
the backend verifies it. There is no authorization-code exchange, so no client secret
is loaded, included in `.env.example`, or required from the project owner.

## Security

Never put this secret in Flutter.

Never commit it to Git.

---

# 9. Google Android OAuth Client

## Status

- [ ] Android package name confirmed
- [ ] Debug signing fingerprint configured
- [ ] Release signing fingerprint configured before production

## Required For

Google Sign-In on Android.

## Required Information

- Android package name
- SHA-1 signing certificate fingerprint
- Potentially SHA-256 depending on the integration

Example package format:

```text
com.sayarti.app
```

Use the **actual** package configured in the Flutter project.

## Where To Get SHA-1

For local/debug builds, obtain it from the Android signing configuration/keystore.

For production with Google Play App Signing, use the signing certificate information from Google Play Console when applicable.

## Where To Configure

Create an Android OAuth client in Google Cloud Console using the real package name and signing fingerprint.

## Verification

Google Sign-In must work on a real Android build using the expected package/signing certificate.

---

# 10. Google iOS OAuth Client

## Status

- [ ] iOS Bundle Identifier confirmed
- [ ] iOS OAuth client configured

## Required For

Google Sign-In on iOS.

## Required Information

The actual iOS Bundle Identifier.

Example:

```text
com.sayarti.app
```

## Where To Configure

Create an iOS OAuth client in the Google Cloud project's authentication configuration.

The Bundle Identifier must match the Flutter iOS target exactly.

## Verification

Google Sign-In succeeds from the iOS application and the resulting identity token is accepted by the backend.

---

# 11. Firebase Project

## Status

- [ ] Firebase project created or selected

## Required For

- Firebase Cloud Messaging
- Push notifications

## Where To Get It

Use Firebase Console.

The Firebase project may use the same underlying Google Cloud project as Sayarti when appropriate.

---

# 12. Firebase Admin SDK Credentials

## Status

- [ ] Required from project owner: valid Firebase Admin credentials and real-device delivery verification

## Environment Variable

```env
FIREBASE_SERVICE_ACCOUNT_PATH=
```

## Required For

Sending push notifications from Spring Boot.

## Where To Get It

Firebase Console:

```text
Project Settings
→ Service Accounts
→ Firebase Admin SDK
```

Choose **Generate new private key** and download the Firebase Admin SDK service-account
JSON file.

## Private Key Handling

Store the downloaded JSON outside the repository with permissions restricted to the backend
process and its operator. Set `FIREBASE_SERVICE_ACCOUNT_PATH` to that file's absolute path in
the local, ignored `.env` file or in the process environment. Never commit or copy the JSON
file into this repository.

The backend opens that file and passes its stream directly to
`GoogleCredentials.fromStream(...)`; it does not map credential fields to separate environment
variables. Firebase beans are enabled only when the path identifies a readable, valid
service-account credential. A missing, blank, nonexistent, unreadable, or invalid file leaves
the application startable with the unavailable notification-provider fallback. Notification
attempts then fail safely rather than pretending Firebase is configured.

For production, mount the credential using the hosting platform's secret-file or workload
credential mechanism and set `FIREBASE_SERVICE_ACCOUNT_PATH` to the mounted file. Do not rely
on a developer workstation or Desktop path. Follow the platform's access-control, rotation,
and audit guidance.

## Verification

- [ ] Firebase Admin SDK initializes with owner-provided credentials.
- [ ] A test device is registered with a real FCM registration token.
- [ ] The backend successfully sends a test message through Firebase.
- [ ] Android receives it.
- [ ] iOS receives it after APNs configuration is complete.

Automated tests use temporary generated test credential files and mocked notification-provider
dependencies; they never send a real Firebase message. Real Firebase delivery has therefore
not been verified and must remain pending until the owner completes every applicable step above.

---

# 13. Flutter Firebase Configuration

These are mobile-side files, not backend secrets:

```text
Android: google-services.json
iOS: GoogleService-Info.plist
```

They belong to the Flutter project.

Never copy Firebase Admin service-account credentials into the mobile app.

---

# 14. Apple Push Notification Service (APNs)

## Status

- [ ] APNs setup evaluated
- [ ] Required Apple configuration completed before iOS push testing

## Required For

Receiving Firebase notifications on iOS.

## Potential Required Values

Depending on Firebase/iOS setup:

```text
APNs Authentication Key
Key ID
Apple Team ID
iOS Bundle ID
```

## Where To Get It

Apple Developer account and Firebase Console.

Codex must document exact values and navigation when the iOS notification integration is implemented.

Never invent these values.

---

# 15. CORS Allowed Origins

## Status

- [ ] Production origins confirmed

## Environment Variable

```env
ALLOWED_ORIGINS=
```

Do not hardcode production origins.

Do not default authenticated production APIs to `*` without an explicit decision.

---

# 16. Production Database Credentials

## Status

- [ ] Production database created
- [ ] Production credentials configured

## Environment Variables

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
```

Store production values in the hosting provider's secrets/configuration system.

Never commit them.

---

# 17. Production Deployment Configuration

## Status

- [ ] Hosting provider selected
- [ ] Production environment variables configured
- [ ] Production API base URL available

When a hosting provider is selected, Codex must add any provider-specific required configuration here.

Only document credentials required by actual implemented features.

---

# Missing Key Template

Whenever Codex discovers a new required owner-provided value, append a section using this template:

```md
# <Integration / Credential Name>

## Status

- [ ] Required from project owner

## Required For

<Feature>

## Environment Variable

```env
VARIABLE_NAME=
```

## Why It Is Needed

<Explanation>

## Where To Get It

<Service / Console>

## Steps

1. ...
2. ...
3. ...

## Where To Put It

```env
VARIABLE_NAME=<value>
```

## Backend Usage

<File / configuration / module>

## Verification

1. ...
2. ...
3. ...
```

---

# Owner Action Summary

Potential owner-provided configuration for V1:

- [ ] SQL Server credentials
- [ ] JWT access secret
- [ ] JWT refresh secret
- [ ] Google Cloud project
- [ ] Google OAuth client ID
- [ ] Android Google OAuth configuration
- [ ] iOS Google OAuth configuration
- [ ] Firebase project
- [ ] Firebase Admin credentials
- [ ] APNs configuration for iOS push notifications
- [ ] Production allowed origins
- [ ] Production database credentials
- [ ] Production deployment configuration
- [ ] Transactional email provider credentials and sender identity

Not every item must be available on day one.

Codex should implement everything safely possible without missing credentials and leave clear instructions here for anything the project owner must provide.

---

# 18. SMTP Transactional Email Delivery

## Status

- [ ] Required from project owner
- [x] Spring Mail SMTP adapter implemented and automated-test verified
- [ ] Real email delivery configured and verified

## Required For

Delivering the six-digit LOCAL-account verification OTP. Business and authentication
logic use the provider-neutral `EmailService`; only the SMTP adapter uses Spring Mail.
A different SMTP provider can therefore be used in production without changing
application business logic.

## Environment Variables

```env
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=
SMTP_TLS=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL=false
SMTP_CONNECTION_TIMEOUT=10000
SMTP_TIMEOUT=60000
SMTP_WRITE_TIMEOUT=60000
```

- `SMTP_HOST`: SMTP server hostname supplied by the mail provider.
- `SMTP_PORT`: SMTP submission port. Port 587 is normally used with STARTTLS.
- `SMTP_USERNAME`: SMTP login name, often the sending email address.
- `SMTP_PASSWORD`: SMTP credential or provider-issued app password; keep it secret.
- `SMTP_FROM`: sender address placed in the message's From header. It must be allowed
  or verified by the provider.
- `SMTP_TLS`: enables STARTTLS, upgrading the SMTP connection to TLS.
- `SMTP_STARTTLS_REQUIRED`: fails delivery unless the server supports STARTTLS.
- `SMTP_SSL`: enables implicit SMTP-over-SSL (commonly port 465). Do not normally
  enable this together with the port-587 STARTTLS configuration.
- `SMTP_CONNECTION_TIMEOUT`: connection timeout in milliseconds (default `10000`).
- `SMTP_TIMEOUT`: socket read timeout in milliseconds (default `60000`).
- `SMTP_WRITE_TIMEOUT`: socket write timeout in milliseconds (default `60000`).

## Gmail SMTP Example For Development

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=<your-google-account-email>
SMTP_PASSWORD=<your-16-character-google-app-password>
SMTP_FROM=<your-google-account-email-or-approved-sender>
SMTP_TLS=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL=false
SMTP_CONNECTION_TIMEOUT=10000
SMTP_TIMEOUT=60000
SMTP_WRITE_TIMEOUT=60000
```

Do not use the normal Google account password as `SMTP_PASSWORD`. Use a Google App
Password:

1. Sign in to the Google account that will send development messages.
2. Open the Google Account security settings and enable **2-Step Verification**.
3. Open **App passwords** (search the Google Account settings if necessary).
4. Create a new App Password for Sayarti/mail.
5. Copy the generated App Password once and store it as `SMTP_PASSWORD` in the local
   uncommitted `.env` or deployment secret manager.
6. Set `SMTP_USERNAME` and `SMTP_FROM` to the appropriate account/sender address.

Google may not show App Passwords for some managed, child, or Advanced Protection
accounts. In that case, use an organization-approved SMTP provider instead.

## Where To Put It

Copy `.env.example` to the ignored `.env` file for local development, or configure
these values in the deployment platform's environment/secrets manager. Never commit
credentials and never put SMTP credentials in Flutter.

Spring maps the connection settings under `spring.mail` and the sender separately as
`sayarti.email.from`. Defaults are safe for application startup, but delivery will
fail until a reachable SMTP server and valid credentials are configured.

## Failure And Transaction Behavior

SMTP/provider failures are converted to the safe application error
`EMAIL_DELIVERY_FAILED`; raw provider messages and credentials are not returned.
Operational logging includes only the recipient domain and exception category and
never includes the OTP or SMTP password.

For registration and resend, the unverified user and newly hashed OTP state are kept
when delivery fails. This avoids losing the registration and leaves a consistent
single active OTP record. The client receives the safe delivery failure and can use
`POST /api/v1/auth/resend-verification` later after the configured cooldown. Plaintext
OTP is passed only to the adapter and is never persisted or logged.

## Real End-to-End Verification

1. Configure all SMTP variables in the local uncommitted `.env` or exported process
   environment.
2. Ensure the development database is running and start the backend with those
   environment variables loaded.
3. Register a new LOCAL account using an inbox you control.
4. Confirm one Sayarti verification message arrives and contains a six-digit OTP.
5. Submit it to `POST /api/v1/auth/verify-email` before expiration and confirm access
   and refresh tokens are returned.
6. Confirm the same code cannot be reused.
7. After the cooldown, test `POST /api/v1/auth/resend-verification`; confirm only the
   newest delivered code works.
8. Only after receipt and verification succeeds should real SMTP delivery be marked
   verified.
