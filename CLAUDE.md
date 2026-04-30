# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test all
./gradlew test

# Single test class
./gradlew test --tests "com.ista.myista.auth.AuthServiceTest"

# Clean
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Environment

Copy `.env.example` to `.env` before running.

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HS256 signing key — min 32 chars |
| `TENANTAPI_HOST` | Base URL of upstream TenantAPI |
| `TENANTAPI_APP_ID` | App identifier sent to TenantAPI |
| `TENANTAPI_KIOSK_USERNAME` | TenantAPI credentials for QuickPay (guest checkout) |
| `TENANTAPI_KIOSK_PASSWORD` | TenantAPI credentials for QuickPay (guest checkout) |
| `CORS_ORIGINS` | Allowed frontend origins |
| `STRIPE_SECRET_KEY` | Stripe secret key (UK, ThamesWey, PrePayment) |
| `STRIPE_PUBLISHABLE_KEY` | Stripe publishable key (sent to frontend) |
| `NGENIUS_URL` | N-Genius API base URL (UAE, Qatar) |
| `NGENIUS_USERNAME/PASSWORD` | N-Genius API credentials |
| `NGENIUS_OUTLET` | N-Genius outlet ID |
| `NGENIUS_WEBHOOK_USERNAME/PASSWORD` | Basic Auth for N-Genius webhook |
| `CBQ_URL` | Commercial Bank Qatar API URL (Qatar QuickPay) |
| `CBQ_MERCHANT_ID/PASSWORD` | CBQ credentials |
| `CBQ_JS_LIB_URL` | CBQ JS SDK URL (sent to frontend) |
| `CBQ_NOTIFICATION_URL` | CBQ callback URL |
| `DB_UKSQL01_URL` | JDBC URL for uksql01 (MinuteView: Help, N-Genius webhooks, consumption projection) |
| `DB_UKSQL01_USERNAME/PASSWORD` | uksql01 credentials |
| `DB_UKBIZ04_URL` | JDBC URL for ukbiz04 (OperationsDW: seasonal consumption) |
| `DB_UKBIZ04_USERNAME/PASSWORD` | ukbiz04 credentials |

Server listens on port **8080**.

## Stack

- **Kotlin 2.1.20 / Java 21**
- **Spring Boot 3.4.4** with Spring Security (stateless JWT)
- **JJWT 0.12.6** for token signing/validation
- **Stripe Java SDK 27.1.0**
- **Spring JDBC + mssql-jdbc 12.8.1** for SQL Server

## Architecture

A **REST API proxy** that authenticates users, issues JWTs, and forwards requests to an external **TenantAPI**. Also integrates directly with payment processors and SQL Server.

### Layers

```
api/          REST controllers — one per domain
auth/         Login/refresh endpoints, JWT filter, JWT service, UserPrincipal DTO
config/       SecurityConfig (public vs. protected routes), WebConfig (CORS), MssqlConfig
mssql/        SQL Server repositories (N-Genius webhook, Consumption, Help)
payment/      PaymentGateway interface + sealed PaymentSession + PaymentGatewayRegistry
  stripe/     StripeGateway — cards, Direct Debit, saved methods
  ngenius/    NGeniusGateway + NGeniusWebhookController + NGeniusWebhookRepository
  cbq/        CBQGateway (Qatar QuickPay)
tenantapi/    TenantApiClient (raw HTTP) + TenantApiService (typed domain methods)
  dto/        Typed response DTOs (UserInfo, Balance, Transaction, MeterInfo, …)
variant/      VariantService — detects app variant from domain or ?variant= param
```

### Payment architecture

**Variant → Gateway mapping** (in `PaymentGatewayRegistry`):

| Variant | Gateway | Currency |
|---------|---------|----------|
| uk, thameswey, prepayment | Stripe | GBP |
| uae, qatar (logged-in) | N-Genius | AED |
| qatar (QuickPay guest) | CBQ | QAR |
| uae (QuickPay guest) | N-Genius (kiosk mode) | AED |
| be | None | — |

**Response shape** — controllers return a sealed `PaymentSession` which Jackson serialises as:
```json
{ "method": "stripe",  "paymentId": "…", "clientSecret": "…", "publishableKey": "…" }
{ "method": "ngenius", "paymentId": "…", "redirectUrl": "…" }
{ "method": "cbq",     "paymentId": "…", "sessionId": "…", "successIndicator": "…", "jsLibUrl": "…" }
```

Frontend switches on `method` to render the appropriate payment UI.

**Stripe flow** (no server-side webhook):
1. `POST /api/payments` → StripeGateway creates TenantAPI pending payment + Stripe PaymentIntent → returns `{ method: "stripe", clientSecret, ... }`
2. Frontend uses Stripe.js to confirm
3. User redirected to `GET /api/payments/status/stripe/{success|failure}?pid=...`
4. Backend calls `paymentStatusUpdate` on TenantAPI

**N-Genius flow** (server-side webhook):
1. `POST /api/payments` → NGeniusGateway creates TenantAPI pending payment + N-Genius order → returns `{ method: "ngenius", redirectUrl }`
2. User redirected to N-Genius hosted page
3. N-Genius POSTs webhook to `POST /payments/webhook/ngenius` (Basic Auth)
4. Backend logs to MSSQL + reconciles via stored procedures (does NOT call TenantAPI)

**CBQ flow** (status poll on redirect):
1. `POST /api/payments` → CBQGateway creates TenantAPI pending payment + CBQ session → returns `{ method: "cbq", sessionId, ... }`
2. Frontend loads CBQ JS SDK and initiates checkout
3. CBQ redirects to `GET /api/payments/status/cbq/{status}?pid=...`
4. Backend queries CBQ order status → calls TenantAPI `paymentStatusUpdate`

**QuickPay (guest checkout, no auth)**:
- Routes: `POST /quickpay/lookup`, `POST /quickpay/payment`, `GET /quickpay/status/{method}/{status}`
- Uses TenantAPI kiosk mode credentials
- UAE: NGeniusGateway in kiosk mode, Qatar: CBQGateway in kiosk mode

### SQL Server integration

Two separate SQL Server connections, each injected by qualifier (`@Qualifier("uksql01Jdbc")` / `@Qualifier("ukbiz04Jdbc")`):

**uksql01** (`MinuteView` database):
- N-Genius webhooks: `MinuteView.mi.NGeniusEventLogInsert`, `MinuteView.mi.NGeniusEventPaymentReconciliation`
- Consumption projection: `uesl.dbo.cspConsumptionProjectionByCustID @CustID, @ServiceTypeId` (cross-DB call on same server)
- Help: `MinuteView.mi.HelpCategorySelectByVariantId`, `MinuteView.mi.HelpArticleSelect`, `MinuteView.mi.HelpArticleSelectByArticleId`, `MinuteView.mi.HelpArticleSearch`

**ukbiz04** (`OperationsDW` database):
- Seasonal consumption: direct T-SQL against `dbo.DimTime`, `dbo.DimMeter`, `dbo.DimUnit`, `dbo.FactConsumptionDaily`

### Auth flow

1. `POST /auth/login` → `AuthService` calls `TenantApiClient.authenticate()` → gets TenantAPI tokens → fetches tenant info → mints JWT with user/tenant claims → returns JWT + refresh token.
2. Every subsequent request: `JwtAuthFilter` validates JWT, populates `SecurityContext` with `UserPrincipal`.
3. `POST /auth/refresh` → mints a new JWT.

### Public routes

- `/auth/**` — login, refresh
- `/api/config` — variant detection
- `/quickpay/**` — guest checkout
- `/payments/webhook/**` — payment processor webhooks (N-Genius Basic Auth)

## Testing

Tests in `src/test/kotlin/com/ista/myista/auth/`. JUnit 5 + Mockito-Kotlin.
