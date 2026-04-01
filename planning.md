# NexusVAS — Multi-Tenant Telecom Value-Added Services Platform

## Why This Project

MIAKI's core business revolves around telecom VAS (Value-Added Services), content delivery for mobile operators, and multi-country digital service provisioning. Their portfolio includes products like BDAPPS, RobiTube, mHealth, GoGames, and Kheli — all of which are content/service platforms built on top of telecom infrastructure.

This project replicates that exact domain: a backend platform that allows multiple telecom operators (Robi, GP, Banglalink, etc.) to onboard, manage, and deliver value-added services (game subscriptions, health tips, video content, sticker packs) to their subscribers, with real-time billing, event-driven content delivery, and operator-level analytics.

---

## Core Domain Concepts

| Concept | Description |
|---|---|
| Operator (Tenant) | A telecom company (e.g., Robi, GP) — each is an isolated tenant |
| Subscriber | An end-user identified by MSISDN (phone number) |
| VAS Product | A service offering — game pack, health tips, video bundle, sticker pack |
| Subscription | A subscriber's active enrollment in a VAS product (daily/weekly/monthly) |
| Billing Event | A charge/refund event against a subscriber's operator balance |
| Content Item | Actual deliverable content — video, image, text, game link |
| Campaign | A promotional push to drive subscriptions via SMS/USSD |

---

## Technology Mapping to JD Requirements

| JD Requirement | How It's Used |
|---|---|
| Java 21 + OOP/Concurrency | Core language across all services |
| Spring Boot 3.x | Foundation for every microservice |
| Spring MVC | REST APIs for operator portal, admin, subscription management |
| Spring WebFlux | Reactive content streaming endpoint, real-time billing callbacks |
| Spring Data JPA | Entity mapping for operators, products, subscriptions, ledger |
| Spring Security + OAuth2 + JWT | Dedicated Auth Service (Spring Authorization Server), multi-tenant RBAC, API key exchange, OTP, inter-service Client Credentials |
| Kafka | Core event backbone — subscription events, billing events, content delivery triggers |
| RabbitMQ | Notification dispatch (SMS, USSD push), campaign task queues with DLQ |
| PostgreSQL | Primary transactional store — operators, products, subscriptions, billing ledger |
| MongoDB | Content catalog, subscriber activity logs, analytics snapshots |
| Redis | Rate limiting per operator API key, subscription status cache, idempotency keys for billing |
| Docker | Containerized services — production Docker Compose on 6GB VPS with memory budgets |
| Kubernetes | Cloud-ready Helm charts, HPA, Strimzi Kafka, NetworkPolicies, Ingress + TLS (showcase) |
| REST API Versioning | `/api/v1/`, `/api/v2/` — operator-facing APIs |
| SpringDoc OpenAPI (Swagger) | Per-service Swagger UI, aggregated via Gateway, security schemes, API contract validation in CI |
| API Security (OAuth2/JWT) | Dedicated Auth Service — operator API key exchange, admin portal login, subscriber OTP, service-to-service Client Credentials, JWKS, RBAC policy engine |
| GraphQL | Analytics and reporting queries for operator dashboard |
| Event Sourcing / CQRS | Subscription lifecycle as event stream; read-optimized query models |
| Distributed Tracing | Log-based correlation tracing with Micrometer (Zipkin in dev only — too heavy for 6GB VPS) |
| CI/CD | GitHub Actions — build + test on runners, SSH deploy to VPS, Helm deploy to K8s |
| High-load Systems | Designed for millions of subscription/billing events per day per operator |
| FinTech Relevance | Real-time billing, double-entry ledger, reconciliation |
| Flyway | Version-controlled database migrations per service, partitioning scripts, CI validation, zero-downtime schema evolution |
| Spring AI + OpenRouter | Churn prediction, smart content recommendations, campaign copy generation, RAG-powered subscriber support, fraud detection — multi-model routing via OpenRouter (Gemini, Claude, GPT) |
| PGVector | Vector store for RAG — embeds product docs, FAQs, and operator knowledge base for AI-powered search |

---

## Microservices Architecture

```
                        ┌──────────────┐
         Client Apps ──▶│  API Gateway  │◀── Rate Limit (Redis)
                        └──────┬───────┘
                               │
                     ┌─────────┴─────────┐
                     ▼                   │
              ┌────────────┐             │
              │   Auth      │             │
              │  Service    │◀── Redis    │
              │ (OAuth2/JWT)│    (sessions │
              └─────┬──────┘    + tokens) │
                    │ JWT                 │
          ┌────────┼┬────────┬───────────┼────────┐
          ▼        ▼▼        ▼           ▼        ▼
   ┌────────────┐ ┌────────┐ ┌──────┐ ┌──────────┐ ┌────────┐
   │  Operator   │ │  Sub   │ │Billing│ │ Content  │ │Campaign│
   │  Service    │ │Service │ │Service│ │ Service  │ │Service │
   └─────┬──────┘ └───┬────┘ └──┬───┘ └────┬─────┘ └───┬────┘
         │             │         │           │           │
         │        Kafka Topics   │       MongoDB     RabbitMQ
         │     ┌───────┴─────────┤                      │
         ▼     ▼                 ▼                      ▼
   ┌──────────────┐      ┌────────────┐         ┌────────────┐
   │  PostgreSQL   │      │  Analytics  │         │Notification│
   │  (per-tenant) │      │  Service    │         │  Service   │
   └──────────────┘      └────────────┘         └────────────┘
                               │
                    Kafka Events│
                               ▼
                      ┌──────────────┐
                      │  AI Service   │◀── Spring AI + PGVector
                      │ (Spring AI)   │◀── OpenRouter API
                      └──────────────┘
```

### Service Breakdown

1. **API Gateway** — Spring Cloud Gateway, routes requests, delegates auth to Auth Service, enforces Redis-based rate limits per operator
2. **Auth Service** — Dedicated OAuth2 Authorization Server (Spring Authorization Server), issues/validates/revokes JWT tokens, manages users/roles/permissions, handles operator API key exchange, subscriber OTP login, admin login, RBAC policy engine
3. **Operator Service** — Tenant onboarding, API key management, operator config (billing model, currency, timezone)
4. **Subscription Service** — Subscribe/unsubscribe/renew commands, Event Sourcing for subscription lifecycle, publishes to Kafka
5. **Billing Service** — Charges operator balance, double-entry ledger, idempotent billing via Redis, publishes billing events
6. **Content Service** — Content catalog in MongoDB, content delivery via reactive WebFlux streaming, CDN URL generation
7. **Campaign Service** — Bulk SMS/USSD campaign scheduling, RabbitMQ task distribution, A/B targeting
8. **Notification Service** — Consumes RabbitMQ messages, dispatches SMS/push, DLQ for failures, retry with backoff
9. **Analytics Service** — Consumes all Kafka events, stores in MongoDB, serves GraphQL queries for dashboards
10. **AI Service (Spring AI)** — Churn prediction, content recommendations, campaign copy generation, RAG-powered subscriber support chatbot, billing fraud detection — powered by OpenRouter

---

## Step-by-Step Implementation

### Implementation Status
- ✅ Phase 1 — Project Skeleton & Infrastructure (complete)
- ✅ Phase 1B — Flyway Migrations (complete)
- ✅ Phase 2 — Auth Service (complete: entities, repositories, SecurityConfig, AuthService, TokenService, RbacService, ApiKeyService, OtpService, AuditService, REST controllers, Flyway V10–V11)
- ✅ Phase 3 — Subscription Service (complete: SubscriptionAggregate, Event Sourcing, Outbox, CQRS projections, Kafka Saga, REST controllers)
- ✅ Phase 4 — Billing Service (complete: double-entry ledger, idempotent charging, Kafka Saga, refund, REST controllers)
- ⬜ Phase 5 — Content Service
- ⬜ Phase 6 — Campaign + Notification Services
- ⬜ Phase 7 — Analytics + AI Services
- ⬜ Phase 8 — API Gateway + Operator Service
- ⬜ Phase 9 — CI/CD + Kubernetes

---

### Phase 1 — Project Skeleton & Infrastructure

#### Step 1: Create Multi-Module Maven Project

- Set up a parent `pom.xml` with Spring Boot 3.x as the parent
- Create child modules: `api-gateway`, `auth-service`, `operator-service`, `subscription-service`, `billing-service`, `content-service`, `campaign-service`, `notification-service`, `analytics-service`, `ai-service`, `common-lib`
- In `common-lib`, define shared event envelope class, custom exception hierarchy, common DTOs, and utility classes
- Configure Maven profiles for `dev`, `staging`, `prod`

#### Step 2: Write Docker Compose for Local Development

- Define services: PostgreSQL (7 databases — auth, operator, subscription, billing, campaign, notification, ai) with PGVector extension enabled, MongoDB, Redis, Kafka (KRaft mode — no Zookeeper), RabbitMQ (with management UI), Zipkin
- **Why Kafka KRaft, not Zookeeper?**
  - **RAM**: Zookeeper is a separate Java process requiring its own JVM heap (256–512MB idle). On a 6GB VPS, that's ~300MB you can't afford. KRaft eliminates Zookeeper entirely — Kafka handles metadata consensus internally within the same broker process. One fewer container, one fewer JVM.
  - **Architecture**: Apache Kafka deprecated Zookeeper in Kafka 3.5 (2023) and removed it entirely in Kafka 4.0 (early 2025). KRaft is now the default and only supported mode. Zookeeper was an operational burden — a separate distributed system to deploy, monitor, and tune alongside Kafka. It introduced split-brain risks, added latency to controller elections, and limited partition scalability (~200K partition ceiling). KRaft moved metadata management into Kafka's own Raft consensus protocol, resulting in faster broker startup, faster controller failover, simpler operations, and a single system to manage instead of two.
  - Use the `apache/kafka` Docker image (Kafka 4.x) which is KRaft-native — no Zookeeper image needed in Docker Compose at all
- Set up named volumes for all stateful services
- Create an `.env` file for configurable ports, credentials, topic names, and `OPENROUTER_API_KEY` for the AI Service
- Add a health check for each infrastructure service
- Expose RabbitMQ Management UI on port 15672 and Zipkin on port 9411

#### Step 3: Define Domain Events

- Create the base event envelope in `common-lib`: `eventId (UUID)`, `eventType (String)`, `tenantId (String)`, `aggregateId (String)`, `timestamp (Instant)`, `version (int)`, `payload (JsonNode)`
- Define domain events:
  - Operator domain: `OperatorOnboarded`, `OperatorSuspended`, `ApiKeyRotated`
  - Auth domain: `UserRegistered`, `UserLocked`, `RoleAssigned`, `RoleRevoked`, `TokenRevoked`, `ApiKeyCreated`, `ApiKeyRevoked`, `PermissionDenied`
  - Subscription domain: `SubscriptionCreated`, `SubscriptionRenewed`, `SubscriptionCancelled`, `SubscriptionExpired`
  - Billing domain: `ChargeInitiated`, `ChargeSucceeded`, `ChargeFailed`, `RefundIssued`
  - Content domain: `ContentPublished`, `ContentDelivered`, `ContentExpired`
  - Campaign domain: `CampaignLaunched`, `CampaignCompleted`, `CampaignFailed`
  - AI domain: `ChurnRiskScored`, `RecommendationGenerated`, `FraudFlagged`, `CampaignCopyGenerated`

---

### Phase 1B — Database Migration Strategy with Flyway

Every PostgreSQL-backed microservice owns its own schema and migrations. Flyway ensures that database changes are version-controlled, repeatable, and environment-safe — no manual DDL execution, ever.

#### Step 3A: Set Up Flyway Per Microservice

- Add `flyway-core` dependency to every service that uses PostgreSQL: `auth-service`, `operator-service`, `subscription-service`, `billing-service`, `campaign-service`, `notification-service`, `ai-service`
- In each service, create the migration directory structure:
  ```
  src/main/resources/
  └── db/
      └── migration/
          ├── V1__initial_schema.sql
          ├── V2__add_indexes.sql
          ├── V3__add_audit_columns.sql
          └── R__refresh_views.sql      (repeatable migration)
  ```
- Configure Flyway in each service's `application.yml`:
  - `spring.flyway.enabled: true`
  - `spring.flyway.locations: classpath:db/migration`
  - `spring.flyway.baseline-on-migrate: true` (for existing databases)
  - `spring.flyway.schemas: ${service-specific-schema}` (e.g., `auth`, `operator`, `subscription`, `billing`)
  - `spring.flyway.table: flyway_schema_history` (default tracking table)
- Each service runs its own migrations on startup — no shared migration runner

#### Step 3B: Write Migration Scripts for Each Service

- **Auth Service** (`auth-db`):
  - `V1__create_users_table.sql` — `users` table: `id (UUID PK)`, `email (UNIQUE)`, `password_hash`, `user_type (ENUM: ADMIN, OPERATOR_USER, SUBSCRIBER)`, `tenant_id (nullable)`, `status (ENUM: ACTIVE, LOCKED, SUSPENDED)`, `mfa_secret`, `failed_login_count`, `locked_until`, `created_at`, `updated_at`
  - `V2__create_roles_and_permissions.sql` — `roles` table: `id`, `name`, `tenant_id (nullable — null for global roles)`, `description`, `is_system (boolean)`. `permissions` table: `id`, `name (e.g., subscription:write)`, `resource`, `action`, `description`. Join tables: `user_roles (user_id, role_id)`, `role_permissions (role_id, permission_id)`
  - `V3__create_oauth2_tables.sql` — `oauth2_registered_clients` table: `id`, `client_id (UNIQUE)`, `client_secret_hash`, `client_name`, `grant_types (JSONB)`, `redirect_uris (JSONB)`, `scopes (JSONB)`, `tenant_id`, `created_at`. `oauth2_authorizations` table managed by Spring Authorization Server schema
  - `V4__create_api_keys_table.sql` — `api_keys` table: `id (UUID PK)`, `tenant_id`, `key_hash (SHA-256)`, `name`, `scopes (JSONB)`, `rate_limit_tier`, `issued_at`, `expires_at`, `revoked_at`, `is_active`, `last_used_at`
  - `V5__create_audit_log.sql` — `auth_audit_log` table: `id`, `user_id`, `action (LOGIN, LOGOUT, TOKEN_ISSUED, TOKEN_REVOKED, ROLE_ASSIGNED, PERMISSION_DENIED, MFA_ENABLED, API_KEY_CREATED)`, `resource`, `ip_address`, `user_agent`, `metadata (JSONB)`, `created_at`
  - `V6__seed_system_roles_and_permissions.sql` — insert default roles (`SUPER_ADMIN`, `OPERATOR_ADMIN`, `OPERATOR_VIEWER`, `SUBSCRIBER`) and default permission catalog (`subscription:read`, `subscription:write`, `billing:read`, `billing:write`, `campaign:create`, `content:manage`, `analytics:view`, `ai:invoke`, `operator:manage`, `user:manage`)
  - `V7__add_auth_indexes.sql` — indexes on `users(email)`, `users(tenant_id, status)`, `api_keys(key_hash)`, `api_keys(tenant_id, is_active)`, `auth_audit_log(user_id, created_at)`, `auth_audit_log(tenant_id, action, created_at)`

- **Operator Service** (`operator-db`):
  - `V1__create_operators_table.sql` — `operators` table with `id (UUID PK)`, `name`, `country`, `timezone`, `currency`, `status`, `created_at`, `updated_at`
  - `V2__create_operator_configs_table.sql` — `operator_configs` table with `operator_id (FK)`, `billing_model`, `retry_policy_json (JSONB)`, `rate_limit_tier`, `ai_token_budget`
  - `V3__add_operator_indexes.sql` — indexes on `status`, `country`

- **Subscription Service** (`subscription-db`):
  - `V1__create_event_store.sql` — `subscription_events` table: `event_id (UUID PK)`, `aggregate_id (UUID)`, `tenant_id`, `event_type`, `payload (JSONB)`, `version (int)`, `created_at (timestamptz)` with unique constraint on `(aggregate_id, version)`
  - `V2__create_outbox_table.sql` — `outbox` table: `id (UUID PK)`, `aggregate_id`, `tenant_id`, `event_type`, `payload (JSONB)`, `published (boolean DEFAULT false)`, `created_at`
  - `V3__create_subscription_view.sql` — `subscription_view` read projection table: `id`, `tenant_id`, `subscriber_msisdn`, `product_id`, `product_name`, `status`, `billing_cycle`, `next_renewal_date`, `created_at`, `updated_at`
  - `V4__partition_event_store_by_month.sql` — convert `subscription_events` to range-partitioned table on `created_at`, create initial monthly partitions
  - `V5__add_subscription_view_indexes.sql` — composite indexes on `(tenant_id, status)`, `(tenant_id, subscriber_msisdn)`, `(tenant_id, product_id, status)`
  - `R__create_partition_maintenance_function.sql` — repeatable migration that creates/replaces a function to auto-create future monthly partitions

- **Billing Service** (`billing-db`):
  - `V1__create_billing_accounts.sql` — `billing_accounts` table: `id (UUID PK)`, `tenant_id`, `subscriber_msisdn`, `balance (DECIMAL)`, `currency`, `created_at`
  - `V2__create_ledger_entries.sql` — `ledger_entries` table: `entry_id (UUID PK)`, `tenant_id`, `account_id (FK)`, `counterpart_account_id (FK)`, `amount (DECIMAL)`, `currency`, `entry_type (DEBIT/CREDIT)`, `reference_id`, `description`, `created_at`
  - `V3__create_revenue_accounts.sql` — `operator_revenue_accounts` table for the double-entry credit side: `id`, `tenant_id`, `account_type`, `balance`, `currency`
  - `V4__add_billing_constraints.sql` — CHECK constraint on `ledger_entries` to ensure `amount > 0`, partial unique index on `reference_id` for idempotency at the DB level
  - `V5__partition_ledger_by_month.sql` — range partition `ledger_entries` on `created_at`
  - `V6__add_billing_indexes.sql` — indexes on `(tenant_id, subscriber_msisdn)`, `(reference_id)`, `(created_at)`

- **Campaign Service** (`campaign-db`):
  - `V1__create_campaigns_table.sql` — `campaigns` table: `id (UUID PK)`, `tenant_id`, `name`, `target_criteria (JSONB)`, `channel (SMS/USSD)`, `message_template`, `scheduled_at`, `status`, `created_by`, `created_at`
  - `V2__create_campaign_batches.sql` — `campaign_batches` table: `id`, `campaign_id (FK)`, `batch_number`, `subscriber_count`, `status`, `dispatched_at`
  - `V3__create_campaign_results.sql` — `campaign_delivery_results` table: `id`, `campaign_id (FK)`, `subscriber_msisdn`, `delivery_status`, `delivered_at`, `failure_reason`

- **Notification Service** (`notification-db`):
  - `V1__create_notification_log.sql` — `notification_log` table: `id (UUID PK)`, `tenant_id`, `subscriber_msisdn`, `channel`, `message_body`, `status (SENT/FAILED/RETRYING)`, `retry_count`, `sent_at`, `failure_reason`
  - `V2__add_notification_indexes.sql` — indexes on `(tenant_id, status)`, `(subscriber_msisdn, sent_at)`

- **AI Service** (`ai-db`):
  - `V1__enable_pgvector_extension.sql` — `CREATE EXTENSION IF NOT EXISTS vector;`
  - `V2__create_document_chunks.sql` — `document_chunks` table: `id (UUID PK)`, `tenant_id`, `document_id`, `chunk_index (int)`, `content (TEXT)`, `embedding (vector(1536))`, `source`, `metadata (JSONB)`, `created_at`
  - `V3__create_churn_scores.sql` — `churn_scores` table: `id`, `tenant_id`, `subscriber_msisdn`, `risk_score (int)`, `risk_level`, `top_factors (JSONB)`, `recommended_action`, `scored_at`
  - `V4__create_ai_prompt_log.sql` — `ai_prompt_log` table: `id`, `tenant_id`, `endpoint`, `model`, `prompt_template`, `token_count_input`, `token_count_output`, `latency_ms`, `cost_usd`, `created_at`
  - `V5__add_vector_index.sql` — create an IVFFlat or HNSW index on the `embedding` column for fast similarity search: `CREATE INDEX ON document_chunks USING hnsw (embedding vector_cosine_ops)`
  - `V6__add_ai_indexes.sql` — composite indexes on `(tenant_id, document_id)` for document chunks, `(tenant_id, scored_at)` for churn scores

#### Step 3C: Configure Environment-Specific Flyway Behavior

- **Local / Dev**:
  - `spring.flyway.clean-disabled: false` — allow `flyway:clean` for fast reset during development
  - Use Flyway Maven plugin for CLI commands: `mvn flyway:migrate`, `mvn flyway:info`, `mvn flyway:repair`
  - In Docker Compose, each PostgreSQL database is created via an `init.sql` entrypoint script that runs `CREATE DATABASE auth_db; CREATE DATABASE operator_db; CREATE DATABASE subscription_db; CREATE DATABASE billing_db; CREATE DATABASE campaign_db; CREATE DATABASE notification_db; CREATE DATABASE ai_db;`
- **Staging / Production**:
  - `spring.flyway.clean-disabled: true` — never allow schema wipe
  - `spring.flyway.out-of-order: false` — enforce strict version ordering
  - `spring.flyway.validate-on-migrate: true` — detect tampered migrations
  - `spring.flyway.baseline-on-migrate: true` only for first-time adoption on existing databases; disable after baseline is established

#### Step 3D: Migration Naming Conventions & Team Workflow

- **Naming convention**: `V{YYYYMMDD_HHmm}__{description}.sql`
  - Example: `V20260401_1430__add_subscriber_email_column.sql`
  - This avoids version collisions when multiple developers create migrations in parallel
- **Repeatable migrations** (prefix `R__`): Use for views, functions, stored procedures that can be safely re-applied
  - Example: `R__refresh_subscription_materialized_view.sql`
- **Callback hooks**:
  - Create `afterMigrate.sql` scripts for seeding reference data (e.g., default operator configs, enum lookup tables, test operators for staging)
  - Use `beforeValidate` callback to log migration execution start
- **Code review rule**: Every PR that changes a JPA entity MUST include a corresponding Flyway migration script — JPA `ddl-auto` is set to `validate` (not `update` or `create`) in all environments to catch drift
- **Rollback strategy**: Flyway Community Edition does not support automatic undo. Instead:
  - For additive changes (new columns, tables) — no rollback needed, they're backward-compatible
  - For destructive changes (drop column, rename) — write a manual compensating migration `V{next}__rollback_description.sql` and keep it ready but not committed until needed
  - Use feature flags or column deprecation (add new column → migrate data → deprecate old column in next release) for zero-downtime migrations

#### Step 3E: Integrate Flyway with CI/CD Pipeline

- **CI Pipeline (GitHub Actions)**:
  - Add a `flyway-validate` job that runs before integration tests
  - Spin up a Testcontainers PostgreSQL instance, run `flyway migrate`, then run `flyway validate` — this catches broken or conflicting migrations before merge
  - Fail the build if any migration checksum mismatch is detected
- **CD Pipeline**:
  - Migrations run automatically on service startup (Spring Boot auto-applies Flyway on `ApplicationContext` initialization)
  - In Kubernetes, set the service's `readinessProbe` initialDelaySeconds high enough for migrations to complete before traffic is routed
  - For large/slow migrations (e.g., backfilling millions of rows), use a separate Kubernetes `Job` manifest that runs the migration independently from the service deployment, ensuring the service doesn't block on startup
- **Migration monitoring**:
  - Query `flyway_schema_history` table in each database to verify migration state
  - Add a Spring Actuator custom health indicator that checks Flyway status — reports `DOWN` if pending migrations exist that haven't been applied

---

### Phase 2 — Authorization Service, Identity & Multi-Tenancy

#### Step 4: Build the Authorization Service (Dedicated Microservice)

- Create the `auth-service` module with Spring Boot, **Spring Authorization Server**, Spring Security, Spring Data JPA, and Redis
- This is a fully dedicated service — it is the **single source of truth** for identity, authentication, authorization, and token management across the entire platform
- Design the `auth-db` PostgreSQL schema:
  - `users` — `id (UUID PK)`, `email`, `password_hash (BCrypt)`, `user_type (ENUM: ADMIN, OPERATOR_USER, SUBSCRIBER)`, `tenant_id (nullable — null for super admins)`, `status (ACTIVE/LOCKED/SUSPENDED)`, `mfa_secret`, `created_at`, `updated_at`
  - `roles` — `id`, `name (e.g., SUPER_ADMIN, OPERATOR_ADMIN, OPERATOR_VIEWER, SUBSCRIBER)`, `description`
  - `user_roles` — join table: `user_id (FK)`, `role_id (FK)`
  - `permissions` — `id`, `name (e.g., subscription:write, billing:read, campaign:create, analytics:view, ai:invoke)`, `resource`, `action`
  - `role_permissions` — join table: `role_id (FK)`, `permission_id (FK)`
  - `oauth2_registered_clients` — stores registered OAuth2 client applications (operator portals, internal services, mobile apps) with `client_id`, `client_secret_hash`, `grant_types`, `redirect_uris`, `scopes`, `tenant_id`
  - `oauth2_authorizations` — active authorization sessions (managed by Spring Authorization Server)
  - `api_keys` — `id`, `tenant_id`, `key_hash (SHA-256)`, `name`, `scopes (JSONB)`, `rate_limit_tier`, `issued_at`, `expires_at`, `revoked_at`, `is_active`
  - `audit_log` — `id`, `user_id`, `action`, `resource`, `ip_address`, `user_agent`, `timestamp`

#### Step 4A: Implement Authentication Flows

- **Operator API Client Authentication (Machine-to-Machine)**:
  - Operator sends API key in `X-API-Key` header → API Gateway forwards to Auth Service's `/api/v1/auth/token/api-key` endpoint
  - Auth Service validates the key hash against `api_keys` table, checks `is_active`, `expires_at`, and rate limit tier
  - On success, issues a short-lived JWT (15 min) using Spring Authorization Server with claims: `sub`, `tenant_id`, `scopes`, `client_type: OPERATOR_API`
  - Returns the JWT to the Gateway, which attaches it to all downstream service calls
- **Operator Portal Login (Human Users — Admin/Viewer)**:
  - Operator users hit `POST /api/v1/auth/login` with email + password
  - Auth Service validates credentials (BCrypt), checks account status, enforces rate limiting on failed attempts (Redis counter per email)
  - If MFA is enabled, return a `mfa_required` response with a challenge token → user submits TOTP code to `POST /api/v1/auth/mfa/verify`
  - On success, issue JWT access token (15 min) + opaque refresh token (7 days)
  - Refresh token stored in Redis as `refresh:{tokenHash}` → maps to `userId`, `tenantId`, `deviceId`, with TTL
  - JWT claims: `sub`, `tenant_id`, `roles[]`, `permissions[]`, `client_type: PORTAL_USER`
- **Subscriber Authentication (MSISDN + OTP)**:
  - Subscriber hits `POST /api/v1/auth/subscriber/otp/send` with MSISDN → Auth Service generates 6-digit OTP, stores in Redis (`otp:{msisdn}` with 5-min TTL), publishes to RabbitMQ for SMS delivery via Notification Service
  - Subscriber submits OTP to `POST /api/v1/auth/subscriber/otp/verify` → Auth Service validates against Redis, issues lightweight JWT (1 hour)
  - JWT claims: `sub: msisdn`, `tenant_id`, `roles: [SUBSCRIBER]`, `client_type: SUBSCRIBER`
- **Inter-Service Authentication (Service-to-Service)**:
  - Internal microservices authenticate using OAuth2 Client Credentials Grant
  - Each service is registered as an OAuth2 client in `oauth2_registered_clients` with grant type `client_credentials`
  - Services call Auth Service's `/oauth2/token` endpoint on startup, cache the access token, and attach it to internal REST calls
  - JWT claims include `client_type: INTERNAL_SERVICE`, `service_name`, and a restricted scope set

#### Step 4B: Implement Token Management & Revocation

- **Token Signing**: Use asymmetric RS256 keys — Auth Service holds the private key for signing, all other services hold only the public key (served via `GET /api/v1/auth/.well-known/jwks.json`)
- **Token Refresh**: `POST /api/v1/auth/token/refresh` — validates refresh token in Redis, issues new access token, optionally rotates the refresh token (refresh token rotation for security)
- **Token Revocation**: `POST /api/v1/auth/token/revoke` — deletes the refresh token from Redis, adds the access token's `jti` to a Redis blacklist set (`blacklist:{jti}` with TTL = remaining access token lifetime)
- **Logout**: Revokes both access and refresh tokens, clears the session from Redis
- **Force Logout (Admin Action)**: Admin can revoke all tokens for a specific user — deletes all `refresh:*` entries for that user, bulk-adds all active `jti` values to the blacklist

#### Step 4C: Implement the RBAC Policy Engine

- Build a permission evaluation service inside Auth Service:
  - Given a `userId` (or extracted from JWT), resolve their `roles` → `permissions` → check if the requested `resource:action` is permitted
  - Cache resolved permissions in Redis per user (`perms:{userId}` → `Set<String>`) with a TTL of 10 minutes. Invalidate on role/permission changes.
- **Policy Endpoints** (internal, consumed by other services):
  - `POST /api/v1/auth/policy/check` — accepts `{ userId, tenantId, resource, action }` → returns `{ allowed: boolean, reason }` 
  - `GET /api/v1/auth/policy/permissions/{userId}` — returns the full resolved permission set
- **Pre-built role templates**:
  - `SUPER_ADMIN` — full access to all tenants and all resources
  - `OPERATOR_ADMIN` — full access within their tenant (subscription, billing, campaign, content, analytics, AI)
  - `OPERATOR_VIEWER` — read-only access within their tenant
  - `SUBSCRIBER` — self-service only (own subscriptions, own content)
- Operators can create custom roles via `POST /api/v1/auth/roles` by combining permissions from the permission catalog

#### Step 4D: Integrate Auth Service with API Gateway

- Configure Spring Cloud Gateway to intercept every incoming request:
  - Extract the bearer token (or API key) from the `Authorization` (or `X-API-Key`) header
  - Forward to Auth Service for validation — or validate the JWT locally using the JWKS public key endpoint (preferred for performance)
  - Check the access token's `jti` against the Redis blacklist (for revoked tokens)
  - Extract `tenant_id`, `roles`, `permissions` from the JWT and inject them into request headers (`X-Tenant-Id`, `X-User-Roles`, `X-User-Permissions`) for downstream services
- Implement a `TenantResolutionFilter` that populates a `TenantContext` (ThreadLocal or Reactor Context) from these headers
- Enforce route-level access control in the Gateway: certain routes (e.g., `/api/v1/admin/**`) require `SUPER_ADMIN` or `OPERATOR_ADMIN` role

#### Step 5: Build the Operator Service

- Design PostgreSQL tables: `operators` (id, name, country, timezone, currency, status, created_at), `operator_configs` (billing_model, retry_policy, rate_limit_tier, ai_token_budget)
- Implement operator onboarding REST endpoint — accepts operator details, then calls Auth Service internally to create the operator's OAuth2 client registration, initial admin user, and API key
- Implement operator suspension/activation with event publishing to Kafka — on suspension, Auth Service revokes all tokens for that tenant
- Add pagination and filtering to the operator listing endpoint

#### Step 6: Configure All Downstream Services as Resource Servers

- Every microservice (Subscription, Billing, Content, Campaign, Notification, Analytics, AI) is configured as an **OAuth2 Resource Server**
- Each service validates incoming JWTs using the Auth Service's JWKS endpoint (`spring.security.oauth2.resourceserver.jwt.jwk-set-uri`)
- Implement a shared `SecurityConfig` in `common-lib` that:
  - Extracts `tenant_id` from the JWT and populates `TenantContext`
  - Maps JWT `permissions` claim to Spring Security `GrantedAuthority` objects
  - Enables method-level security with `@PreAuthorize("hasPermission('subscription', 'write')")` using a custom `PermissionEvaluator`
- For critical operations (e.g., delete subscription, issue refund), the service makes a synchronous call to Auth Service's `/api/v1/auth/policy/check` for real-time policy evaluation (belt-and-suspenders approach)

#### Step 7: Set Up Row-Level Tenant Isolation in PostgreSQL

- Add a `tenant_id` column to every shared table in every service database
- Implement a Hibernate `@Filter` or JPA `Specification` in `common-lib` that automatically appends `WHERE tenant_id = :currentTenant` to all queries — the `currentTenant` value is read from `TenantContext` (populated by the JWT claims via Auth Service)
- Write integration tests to verify that Tenant A cannot access Tenant B's data
- Consider schema-per-tenant for high-isolation operators (separate PostgreSQL schemas per operator)
- Auth Service itself uses `tenant_id` scoping on all tenant-specific tables (`users`, `roles`, `api_keys`, `oauth2_registered_clients`) — super admin queries bypass tenant filtering

---

### Phase 3 — Subscription Service with Event Sourcing & CQRS

#### Step 7: Design the Subscription Event Store

- Create the `subscription_events` table in PostgreSQL:
  - Columns: `event_id (UUID PK)`, `aggregate_id (UUID)`, `tenant_id`, `event_type`, `payload (JSONB)`, `version (int)`, `created_at (timestamptz)`
  - Unique constraint on `(aggregate_id, version)` for optimistic concurrency
  - Index on `(tenant_id, aggregate_id)` for fast lookups
  - Partition by `created_at` (monthly) for performance at scale

#### Step 8: Implement the Subscription Aggregate (Write Side / Command Side)

- Build the `SubscriptionAggregate` root class that reconstructs its state by replaying all events for a given `aggregate_id`
- Implement command handlers:
  - `CreateSubscriptionCommand` → validates product exists, subscriber is eligible, no duplicate active subscription → appends `SubscriptionCreated` event
  - `RenewSubscriptionCommand` → checks subscription is active and within renewal window → appends `SubscriptionRenewed`
  - `CancelSubscriptionCommand` → appends `SubscriptionCancelled`
- Each handler persists the event to the event store and then publishes to the `subscription-events` Kafka topic

#### Step 9: Implement the Transactional Outbox Pattern

- Create an `outbox` table: `id`, `aggregate_id`, `tenant_id`, `event_type`, `payload (JSONB)`, `published (boolean)`, `created_at`
- Within the command handler, persist both the event to the event store AND the outbox entry in a single database transaction
- Build an `OutboxPollerService` — a scheduled Spring task that reads unpublished outbox entries, publishes them to Kafka, and marks them as published
- This guarantees at-least-once delivery even if Kafka is temporarily unavailable

#### Step 10: Build the Subscription Query Service (Read Side / CQRS)

- Create a separate module or service that consumes `subscription-events` from Kafka
- Build read-optimized projection tables in PostgreSQL:
  - `subscription_view` — flattened view with current status, product name, subscriber MSISDN, billing cycle, next renewal date
  - Indexed for common query patterns: by tenant, by status, by product, by subscriber
- Expose REST endpoints for listing, filtering, and searching subscriptions
- Expose a GraphQL endpoint for flexible operator dashboard queries (filter by date range, product, status, with aggregation counts)
- Cache hot subscription lookups in Redis with a TTL of 60 seconds

---

### Phase 4 — Billing Service

#### Step 11: Design the Double-Entry Billing Ledger

- Create tables in PostgreSQL:
  - `billing_accounts` — one per subscriber per operator, tracks balance
  - `ledger_entries` — double-entry: every charge creates a debit on subscriber account and credit on operator revenue account
  - Columns: `entry_id`, `tenant_id`, `account_id`, `counterpart_account_id`, `amount`, `currency`, `entry_type (DEBIT/CREDIT)`, `reference_id`, `created_at`
- Add database-level constraints to prevent negative balances (if required by operator config)

#### Step 12: Implement Idempotent Billing

- Before processing any charge, compute an idempotency key: `hash(tenantId + subscriberMsisdn + productId + billingCycleDate)`
- Check Redis for this key — if present, return the cached result (duplicate request)
- If not present, proceed with the charge, then store the key in Redis with a TTL of 24 hours
- This prevents double-charging on network retries or duplicate Kafka messages

#### Step 13: Build the Billing Event Flow

- Subscription Service publishes `SubscriptionCreated` → Billing Service consumes it
- Billing Service executes the charge:
  - On success → publishes `ChargeSucceeded` to Kafka → Subscription Service marks subscription as active
  - On failure → publishes `ChargeFailed` → Subscription Service marks subscription as failed, triggers retry or cancellation
- Implement a Saga pattern: Subscription creation is only finalized after billing confirmation
- Build a reconciliation batch job (Spring `@Scheduled`) that runs nightly — cross-references subscription events with billing events and flags discrepancies

#### Step 14: Reactive Billing Callback Endpoint

- Build a Spring WebFlux endpoint that telecom operators hit for real-time billing callbacks (charge confirmation/rejection)
- This endpoint must handle high concurrency — use non-blocking I/O, reactive Redis, and reactive PostgreSQL (R2DBC)
- Validate callback signatures, process the result, publish the corresponding event to Kafka

---

### Phase 5 — Content Service

The Content Service is the **delivery engine** of the entire VAS platform. While other services manage business logic (who subscribed, who was billed), the Content Service manages **what subscribers actually receive** — the game they play, the health tip they read, the video they watch, the sticker pack they download. It's the service that creates tangible end-user value.

In MIAKI's real portfolio, this maps directly to products like RobiTube (video content), mHealth (health tips), GoGames (mobile games), mSticker (sticker packs), and Kheli (gaming content). Each of these delivers different content types to different subscriber segments across different operators — that's exactly what this service handles.

#### Step 15: Build the Content Catalog

- **Why MongoDB**: Content items have wildly different schemas — a video has `hlsUrl`, `duration`, `resolution`; a game has `gameUrl`, `ageRating`, `platform`; a health tip has `body`, `category`, `language`. Forcing these into rigid PostgreSQL tables means either wide sparse tables or complex inheritance hierarchies. MongoDB's flexible document model lets each content type define its own shape while sharing common fields.
- Design the base content document structure (all content types share these fields):
  ```
  {
    _id: ObjectId,
    tenantId: "robi",
    type: "game" | "health_tip" | "video" | "sticker_pack" | "article",
    title: "Snake Mania",
    description: "Classic snake game with modern graphics",
    tags: ["casual", "arcade", "family"],
    language: "bn",
    status: "PUBLISHED" | "DRAFT" | "ARCHIVED" | "EXPIRED",
    visibility: "TENANT_ONLY" | "GLOBAL",
    targetProducts: ["game-pack-daily", "premium-bundle"],
    thumbnailUrl: "https://cdn.nexusvas.com/thumb/snake-mania.jpg",
    publishedAt: ISODate,
    expiresAt: ISODate,
    metadata: { ... },  // type-specific fields below
    createdBy: "admin-uuid",
    createdAt: ISODate,
    updatedAt: ISODate
  }
  ```
- Define type-specific `metadata` schemas:
  - **Game**: `{ gameUrl, platform (ANDROID/IOS/WEB), ageRating, fileSize, minOsVersion, developer }`
  - **Health tip**: `{ body (rich text / markdown), category (nutrition/exercise/mental_health/disease_prevention), author, medicalReviewedBy, reviewDate }`
  - **Video**: `{ hlsUrl, mp4Url (fallback), duration (seconds), resolution (360p/480p/720p), subtitleUrls: { bn, en }, fileSize, codec }`
  - **Sticker pack**: `{ stickerUrls: [ { url, emotion, sortOrder } ], artist, packSize, animated (boolean) }`
  - **Article**: `{ body, category, readTimeMinutes, author, sources[] }`
- **MongoDB Indexes**:
  - Text index on `title`, `description`, `tags` for full-text search
  - Compound index on `(tenantId, status, type)` — primary query pattern for operator dashboards
  - Compound index on `(tenantId, targetProducts, status)` — for fetching content eligible for a specific subscription product
  - TTL index on `expiresAt` — MongoDB automatically removes expired content documents
- **Content Management REST APIs** (admin-facing, secured with `OPERATOR_ADMIN` role):
  - `POST /api/v1/content` — create content (validates schema by type, uploads thumbnail to object storage, returns content ID)
  - `GET /api/v1/content?tenantId=&type=&status=&tags=&q=&page=&size=` — paginated listing with filters and full-text search
  - `GET /api/v1/content/{contentId}` — get single content item with full metadata
  - `PUT /api/v1/content/{contentId}` — update content metadata
  - `PATCH /api/v1/content/{contentId}/status` — transition status (DRAFT → PUBLISHED, PUBLISHED → ARCHIVED)
  - `DELETE /api/v1/content/{contentId}` — soft-delete (set status to ARCHIVED)
  - `POST /api/v1/content/bulk-import` — batch import content from CSV/JSON for operators migrating from legacy systems
- **Content Versioning**: When content is updated, store the previous version in a `content_versions` collection for audit trail. This is critical for regulated health content where you need to prove what was shown to subscribers at a given time.
- Publish `ContentPublished`, `ContentArchived`, `ContentExpired` events to Kafka when content status changes — consumed by Analytics Service and AI Service (for re-indexing embeddings)

#### Step 16: Reactive Content Delivery Endpoint

- **Why WebFlux here**: The content delivery endpoint is the **highest-throughput endpoint** in the entire system. Every subscriber interaction (open app → load content) hits this endpoint. Unlike admin CRUD operations (low volume, Spring MVC is fine), delivery needs to handle thousands of concurrent requests with minimal resource consumption. WebFlux's non-blocking model serves more concurrent requests per thread than the thread-per-request model.
- **Delivery Flow** (step-by-step for a single request):
  1. Subscriber app calls `GET /api/v1/delivery/content?productId=game-pack-daily&page=0&size=10` with JWT in header
  2. Extract `subscriberMsisdn` and `tenantId` from JWT claims
  3. **Subscription validation** (Redis first): Check Redis key `sub:{tenantId}:{msisdn}` for active subscription to the requested `productId`. If cache miss, call Subscription Query Service (HTTP), then cache the result with 5-min TTL. If no active subscription → return `403 Forbidden` with error code `SUBSCRIPTION_INACTIVE`
  4. **Fetch content**: Query MongoDB for content matching `(tenantId, targetProducts contains productId, status=PUBLISHED)`, sorted by `publishedAt DESC`, with pagination
  5. **Personalization** (optional): If AI recommendations are available in Redis (`recs:{tenantId}:{msisdn}`), re-order the content list to prioritize recommended items. Merge AI recommendations with chronological content so subscribers see both personalized picks and new releases.
  6. **Build response**: Map MongoDB documents to delivery DTOs — strip admin-only fields (`createdBy`, internal metadata), resolve CDN URLs (replace origin URLs with CDN-prefixed URLs based on operator's CDN config), apply device-specific adaptations (e.g., return `mp4Url` instead of `hlsUrl` for older devices detected via `User-Agent`)
  7. **Publish delivery event**: For each content item returned, publish a `ContentDelivered` event to Kafka (asynchronously, non-blocking) containing `tenantId`, `subscriberMsisdn`, `contentId`, `contentType`, `deliveredAt`. This feeds the Analytics Service and AI recommendation engine.
  8. **Return response** with caching headers: `Cache-Control: private, max-age=300`, `ETag` based on content hash — if subscriber polls again within 5 minutes with matching ETag, return `304 Not Modified`
- **Content Streaming endpoint** (for video/audio):
  - `GET /api/v1/delivery/stream/{contentId}` — returns a `Flux<DataBuffer>` for reactive byte streaming
  - Validates subscription, then proxies the video stream from object storage using WebClient (reactive HTTP client)
  - Supports HTTP Range headers for seek/resume functionality
  - Tracks watch duration by accepting a `POST /api/v1/delivery/stream/{contentId}/progress` heartbeat from the client every 30 seconds
- **CDN Integration**:
  - Content files (videos, images, game APKs) are stored in object storage (S3, MinIO, or operator's own CDN)
  - The Content Service never serves raw files directly in production — it generates **signed CDN URLs** with expiration (e.g., CloudFront signed URLs valid for 1 hour)
  - Each operator can configure their own CDN domain in `operator_configs`, so Robi's subscribers get URLs from `cdn.robi-vas.com` and GP's subscribers get URLs from `cdn.gp-content.com`
- **Offline Content Packs**:
  - `GET /api/v1/delivery/pack/{productId}` — returns a manifest JSON listing all content items for a product with their download URLs and checksums
  - Mobile apps use this manifest to download content for offline access (common in Bangladesh where mobile data is intermittent)
  - Manifest includes a `version` field — app compares versions to know when new content is available

---

### Phase 6 — Campaign & Notification Services

#### Step 17: Build the Campaign Service

- Design campaign entities in PostgreSQL: `campaigns` (id, tenant_id, name, target_criteria_json, channel (SMS/USSD), scheduled_at, status, created_by)
- Implement campaign creation endpoint — accepts targeting criteria (e.g., subscribers in a specific region, inactive for 30+ days)
- When a campaign is triggered (on schedule or manually):
  - Query the Subscription Query Service for matching subscribers
  - Split the subscriber list into batches
  - Publish each batch as a message to a RabbitMQ queue (`campaign.dispatch`)
  - Update campaign status to `IN_PROGRESS`

#### Step 18: Build the Notification Service

- Consume messages from the `campaign.dispatch` RabbitMQ queue
- For each subscriber in the batch, format the SMS/USSD message and call the telecom operator's SMS gateway API
- Implement retry logic with exponential backoff (1s → 2s → 4s → 8s, max 3 retries)
- Configure a Dead Letter Queue (`campaign.dispatch.dlq`) for messages that fail all retries
- Also consume `SubscriptionCreated`, `SubscriptionRenewed` events from a separate RabbitMQ fanout exchange to send transactional notifications (welcome SMS, renewal confirmation)
- Track delivery status per message in PostgreSQL

---

### Phase 7 — Analytics Service with GraphQL

#### Step 19: Build the Event Consumer

- Create a Kafka consumer group that subscribes to ALL domain event topics: `subscription-events`, `billing-events`, `content-events`, `campaign-events`
- Store raw events in MongoDB in a `raw_events` collection — this is your immutable audit log
- Index on `tenantId`, `eventType`, `timestamp` for efficient querying

#### Step 20: Build Aggregation Pipelines

- Using MongoDB aggregation framework or scheduled Spring batch jobs, compute:
  - Daily active subscriptions per operator per product
  - Revenue per operator per day/week/month
  - Content delivery count per content item
  - Campaign conversion rate (subscribers who received SMS → actually subscribed)
  - Churn rate (cancellations / total active)
- Store aggregated results in dedicated MongoDB collections: `daily_metrics`, `product_metrics`, `campaign_metrics`

#### Step 21: Expose GraphQL API

- Set up Spring for GraphQL in the Analytics Service
- Define the schema:
  - `Query { subscriptionMetrics(tenantId, dateRange, productId): SubscriptionMetrics }`
  - `Query { revenueReport(tenantId, dateRange, groupBy): [RevenueDataPoint] }`
  - `Query { campaignPerformance(campaignId): CampaignMetrics }`
  - `Query { contentPopularity(tenantId, limit): [ContentRanking] }`
- Implement resolvers that read from the pre-aggregated MongoDB collections
- Add DataLoader pattern to batch and cache repeated lookups within a single GraphQL request

---

### Phase 8 — Caching, Rate Limiting & Performance

#### Step 22: Implement Redis Caching Strategy

- **Subscription status cache**: When the Subscription Query Service updates a projection, also update Redis (`sub:{tenantId}:{msisdn}` → status + product list). TTL: 5 minutes. Content Service reads this before serving content.
- **Product catalog cache**: Cache active products per tenant. TTL: 15 minutes. Invalidate on product update events.
- **Rate limiting**: Implement a Redis sliding window rate limiter in the API Gateway. Each operator has a configured rate limit tier (e.g., Basic: 100 req/s, Premium: 1000 req/s). Use `MULTI/EXEC` for atomic increment + expire.

#### Step 23: Database Optimization

- PostgreSQL partitioning: Partition `subscription_events` by month, `ledger_entries` by month
- Add composite indexes for common query patterns identified during development
- Configure connection pooling with HikariCP — tune `maximumPoolSize` per service based on load testing
- Enable PostgreSQL `pg_stat_statements` for query analysis
- For MongoDB, create compound indexes on `(tenantId, eventType, timestamp)` for the raw events collection

---

### Phase 8B — API Documentation with SpringDoc OpenAPI (Swagger)

Every REST endpoint in the platform must be documented, testable, and explorable through Swagger UI. This isn't a nice-to-have — it's how operator integration teams, frontend developers, and QA engineers discover and test APIs without reading source code.

#### Step 23A: Set Up SpringDoc OpenAPI Per Microservice

- Add `springdoc-openapi-starter-webmvc-ui` dependency to all Spring MVC services (Auth, Operator, Subscription, Billing, Campaign, Notification)
- Add `springdoc-openapi-starter-webflux-ui` dependency to Spring WebFlux services (Content Service, Billing callback endpoint)
- Configure each service's `application.yml`:
  - `springdoc.api-docs.path: /v3/api-docs`
  - `springdoc.swagger-ui.path: /swagger-ui.html`
  - `springdoc.info.title: NexusVAS - {Service Name} API`
  - `springdoc.info.version: 1.0.0`
  - `springdoc.info.description: {Service-specific description}`
  - `springdoc.default-produces-media-type: application/json`
- Each service hosts its own Swagger UI at `http://{service}:{port}/swagger-ui.html` for local development

#### Step 23B: Annotate All REST Controllers

- Add OpenAPI annotations to every controller and DTO across all services:
  - `@Tag(name = "Subscriptions", description = "Subscription lifecycle management")` on controller classes
  - `@Operation(summary = "Create subscription", description = "Creates a new VAS subscription for a subscriber")` on each endpoint method
  - `@ApiResponse(responseCode = "201", description = "Subscription created")` and `@ApiResponse(responseCode = "409", description = "Active subscription already exists")` for all response codes
  - `@Parameter(description = "Operator tenant ID", required = true)` for path/query params
  - `@Schema(description = "Subscriber phone number", example = "+8801712345678")` on DTO fields with realistic example values
- Group related endpoints using `@Tag` — for services with many endpoints (e.g., Auth Service), use multiple tags: "Authentication", "Token Management", "Role Management", "API Key Management", "Policy Engine"
- Document all error response DTOs: use a shared `ErrorResponse` schema in `common-lib` with `@Schema` annotations showing `code`, `message`, `timestamp`, `traceId`, `details[]`

#### Step 23C: Configure Swagger Security Schemes

- Define security schemes that match the Auth Service's authentication flows:
  - **Bearer JWT**: `@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")` — used by portal users and subscribers
  - **API Key**: `@SecurityScheme(name = "apiKeyAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, parameterName = "X-API-Key")` — used by operator API clients
  - **OAuth2 Client Credentials**: `@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = ...))` — used by inter-service calls
- Apply the appropriate scheme to each endpoint using `@SecurityRequirement(name = "bearerAuth")`
- Configure Swagger UI to show the "Authorize" button — operators can paste their JWT or API key and test endpoints directly from the browser

#### Step 23D: Set Up Aggregated Swagger via API Gateway

- In the API Gateway, add `springdoc-openapi-starter-webflux-ui` and configure it to aggregate OpenAPI specs from all downstream services
- Configure `springdoc.swagger-ui.urls` to list each service's API docs endpoint:
  - `{ name: "Auth Service", url: "/auth-service/v3/api-docs" }`
  - `{ name: "Operator Service", url: "/operator-service/v3/api-docs" }`
  - `{ name: "Subscription Service", url: "/subscription-service/v3/api-docs" }`
  - `{ name: "Billing Service", url: "/billing-service/v3/api-docs" }`
  - `{ name: "Content Service", url: "/content-service/v3/api-docs" }`
  - `{ name: "Campaign Service", url: "/campaign-service/v3/api-docs" }`
  - `{ name: "AI Service", url: "/ai-service/v3/api-docs" }`
- The API Gateway's Swagger UI at `https://api.nexusvas.com/swagger-ui.html` becomes the **single portal** where all APIs are browsable — operators can switch between services using the dropdown selector
- Add a route in the Gateway to proxy each service's `/v3/api-docs` endpoint

#### Step 23E: API Versioning in Swagger

- The platform supports API versioning (`/api/v1/`, `/api/v2/`) — configure SpringDoc to document both versions:
  - Use `springdoc.group-configs` to create separate OpenAPI groups: `v1` and `v2`
  - `v1` group scans controllers in `com.nexusvas.*.controller.v1` package
  - `v2` group scans controllers in `com.nexusvas.*.controller.v2` package
- In Swagger UI, operators can toggle between API versions using the group selector
- Deprecated endpoints in `v1` should be annotated with `@Deprecated` and `@Operation(deprecated = true)` — Swagger UI strikes them through visually

#### Step 23F: Generate API Clients and Static Docs

- Add the `springdoc-openapi-maven-plugin` to each service — generates the `openapi.json` spec file during the Maven build phase
- Store generated specs in a `/docs/api/` directory in the repository — version-controlled
- Use these specs to:
  - Auto-generate client SDKs for operators (Java, Python, TypeScript) using OpenAPI Generator
  - Generate static HTML docs using Redoc or Swagger Codegen for operators who prefer reading docs outside Swagger UI
  - Validate API contracts in CI — compare the generated spec against a baseline to detect unintended breaking changes (use `openapi-diff` tool in GitHub Actions)
- Add a `swagger-contract-test` step in the CI pipeline that fails the build if a breaking change is detected without a version bump

---

### Phase 9 — Observability & Distributed Tracing

#### Step 24: Set Up Distributed Tracing (Lightweight for 6GB VPS)

- **Skip Zipkin/Jaeger in production** — they each consume 200-400MB. On a 6GB VPS, that memory is better spent on application services.
- Instead, implement **log-based correlation tracing**:
  - Add Micrometer Tracing to every microservice (zero additional container overhead — it's a library)
  - Configure trace context propagation: every HTTP request gets a `traceId` and `spanId`, propagated via headers between services
  - Kafka message headers carry `traceId` for async flow tracing
  - Every structured log line includes `traceId`, `spanId`, `tenantId`, `service` — you can reconstruct a full request trace by grepping logs: `docker logs -f gateway | grep "traceId=abc123"`
- **For local development only**: Spin up Zipkin in `docker-compose.dev.yml` (not in prod compose) for visual trace inspection during debugging

#### Step 25: Set Up Metrics and Monitoring

- Add Micrometer + Prometheus metrics to each service
- Expose `/actuator/prometheus` endpoint on every service
- Configure Prometheus to scrape all services
- Build Grafana dashboards:
  - System dashboard: JVM memory, GC pauses, thread count, HTTP request rate/latency per service
  - Business dashboard: subscriptions/min, billing success rate, content deliveries/min, campaign dispatch rate
  - Kafka dashboard: consumer lag per topic, message throughput
- Set up alerts: Kafka consumer lag > 10,000, billing failure rate > 5%, service health check failures

#### Step 26: Centralized Logging (6GB-Friendly)

- Configure all services to output structured JSON logs (Logback with `logstash-logback-encoder`)
- Include `traceId`, `spanId`, `tenantId`, `service`, `level` in every log line — this makes `grep` and `jq` as powerful as a search engine
- **Skip ELK Stack** — Elasticsearch alone needs 1-2GB. Instead:
  - **Option A (simplest)**: Docker JSON log driver with rotation (`max-size: 10m`, `max-file: 5`) + use `docker logs {service} | jq '.traceId == "abc123"'` for searching
  - **Option B (recommended)**: Grafana Loki (~50MB) + Promtail (~30MB) — lightweight log aggregation that integrates with the Grafana dashboard you already have. Query logs with LogQL in Grafana. Total overhead: ~80MB.
- If using Loki, add it to the Prometheus/Grafana stack in Docker Compose — Promtail tails Docker container logs and ships them to Loki

---

### Phase 10 — Containerization & 6GB VPS Deployment Strategy

Your VPS has 6GB available RAM. Kubernetes is off the table (the control plane alone consumes 2+ GB). Instead, the deployment target is **Docker Compose on a single VPS** with aggressive memory tuning, service consolidation, and swap as a safety net. This section is a realistic deployment plan, not a theoretical Kubernetes exercise.

#### Step 27: Memory Budget — The Math

With 6GB available, allocate every megabyte intentionally:

```
╔═══════════════════════════════════════════════════════════════╗
║              6GB VPS MEMORY BUDGET                            ║
╠════════════════════════════════╦══════════════════════════════╣
║ COMPONENT                      ║ MEMORY LIMIT                ║
╠════════════════════════════════╬══════════════════════════════╣
║ Linux OS + Docker Engine       ║ ~400 MB (reserved)          ║
╠════════════════════════════════╬══════════════════════════════╣
║ INFRASTRUCTURE                 ║                              ║
║  PostgreSQL (single instance)  ║  350 MB                     ║
║  MongoDB                       ║  250 MB                     ║
║  Redis                         ║   64 MB                     ║
║  Kafka (KRaft, single broker)  ║  400 MB                     ║
║  RabbitMQ                      ║  180 MB                     ║
║  Subtotal                      ║ ~1,244 MB                   ║
╠════════════════════════════════╬══════════════════════════════╣
║ APPLICATION SERVICES           ║                              ║
║  API Gateway                   ║  180 MB                     ║
║  Auth Service                  ║  192 MB                     ║
║  Operator Service              ║  160 MB                     ║
║  Subscription Service          ║  192 MB                     ║
║  Billing Service               ║  192 MB                     ║
║  Content Service (WebFlux)     ║  192 MB                     ║
║  Campaign Service              ║  160 MB                     ║
║  Notification Service          ║  160 MB                     ║
║  Analytics Service             ║  180 MB                     ║
║  AI Service                    ║  192 MB                     ║
║  Subtotal (10 services)        ║ ~1,800 MB                   ║
╠════════════════════════════════╬══════════════════════════════╣
║ OBSERVABILITY (lightweight)    ║                              ║
║  Prometheus                    ║  128 MB                     ║
║  Grafana                       ║  128 MB                     ║
║  Subtotal                      ║  256 MB                     ║
╠════════════════════════════════╬══════════════════════════════╣
║ SWAP (on disk)                 ║ 2 GB                        ║
╠════════════════════════════════╬══════════════════════════════╣
║ TOTAL RAM USED                 ║ ~3,700 MB                   ║
║ HEADROOM                       ║ ~2,300 MB (for spikes,      ║
║                                ║  OS cache, burst traffic)   ║
╚════════════════════════════════╩══════════════════════════════╝
```

- **No Zipkin in production** — distributed tracing is luxury on 6GB. Use structured JSON logs with `traceId` correlation and Grafana Loki (lightweight) or just `docker logs` with `grep` for debugging.
- **No ELK Stack** — Elasticsearch alone needs 1+ GB. Use Loki (50MB) or file-based logging with log rotation.
- **Single PostgreSQL instance** with 7 databases (auth, operator, subscription, billing, campaign, notification, ai) — not 7 separate PostgreSQL containers. One process, shared memory, 7 logical databases.
- **AI Service uses OpenRouter API** — no local LLM, no GPU, no Ollama container. All inference is remote via HTTP.

#### Step 28: JVM Memory Tuning for Every Service

Each Spring Boot service runs on a JVM that needs careful tuning to stay within its memory limit:

- Set explicit heap size via `JAVA_OPTS` in Docker Compose:
  ```
  JAVA_OPTS: "-Xms64m -Xmx128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100
              -XX:+UseStringDeduplication -XX:MaxMetaspaceSize=80m
              -Xss256k -XX:ReservedCodeCacheSize=32m"
  ```
- **Heap**: `-Xms64m -Xmx128m` — starts at 64MB, caps at 128MB. This is tight but workable for microservices with small request/response payloads. Services handling larger data (Content, Analytics) get `-Xmx160m`.
- **G1GC**: Best garbage collector for small heaps — `-XX:+UseG1GC` with `-XX:MaxGCPauseMillis=100`
- **String deduplication**: `-XX:+UseStringDeduplication` — saves heap on repeated strings (tenant IDs, event types, status enums)
- **Metaspace**: `-XX:MaxMetaspaceSize=80m` — Spring Boot loads many classes; cap metaspace to prevent unbounded growth
- **Thread stacks**: `-Xss256k` (default is 1MB) — reduces per-thread overhead since services don't have deep call stacks
- **Code cache**: `-XX:ReservedCodeCacheSize=32m` — limits JIT compiled code cache
- **Total per service**: ~128MB heap + ~50MB non-heap (metaspace, code cache, thread stacks, direct buffers) = ~180MB per service
- For **WebFlux services** (Content Service, API Gateway): thread count is low (Netty event loop, ~2-4 threads), so memory per thread is less of a concern — they're naturally memory-efficient
- **Spring Boot optimizations**:
  - `spring.main.lazy-initialization: true` — defer bean creation until first use, reduces startup memory spike
  - `spring.jmx.enabled: false` — disable JMX to save ~10MB
  - Disable unused auto-configurations: exclude `DataSourceAutoConfiguration` in services that only use MongoDB, exclude `MongoAutoConfiguration` in services that only use PostgreSQL

#### Step 29: Infrastructure Memory Tuning in Docker Compose

- **PostgreSQL (350MB limit)**:
  - `shared_buffers: 96MB` (25% of the 350MB limit)
  - `effective_cache_size: 192MB`
  - `work_mem: 2MB` (per-query sort/hash memory)
  - `maintenance_work_mem: 32MB`
  - `max_connections: 80` (10 services × 5 HikariCP connections each + buffer)
  - `wal_buffers: 4MB`
  - Enable PGVector extension in the init script
  - All 7 databases share one PostgreSQL process — no per-database memory overhead

- **MongoDB (250MB limit)**:
  - `--wiredTigerCacheSizeGB 0.1` (100MB WiredTiger cache — MongoDB defaults to 50% of RAM which is way too much)
  - `--maxConns 50`
  - `--quiet` to reduce log verbosity

- **Redis (64MB limit)**:
  - `maxmemory 48mb`
  - `maxmemory-policy allkeys-lru` — evict least recently used keys when full
  - Disable RDB persistence in development to save RAM: `save ""`
  - Enable AOF persistence for production: `appendonly yes`, `appendfsync everysec`

- **Kafka KRaft (400MB limit)**:
  - `KAFKA_HEAP_OPTS: "-Xms128m -Xmx256m"` — Kafka's default 1GB is overkill for a single-broker dev/staging setup
  - KRaft mode eliminates Zookeeper entirely — saves ~300MB (one fewer JVM process) and reduces operational complexity (no split-brain, no Zookeeper monitoring)
  - Use `apache/kafka:4.0` image or later — KRaft is the only mode, Zookeeper support has been fully removed
  - Configure `KAFKA_PROCESS_ROLES: broker,controller` and `KAFKA_CONTROLLER_QUORUM_VOTERS` for single-node KRaft
  - `log.retention.hours: 168` (7 days — don't accumulate unlimited log segments)
  - `log.segment.bytes: 52428800` (50MB segments)
  - `num.partitions: 3` (not 12 — fewer partitions = less memory for partition metadata)
  - `log.cleaner.enable: true`

- **RabbitMQ (180MB limit)**:
  - `vm_memory_high_watermark.relative = 0.6` (108MB before flow control kicks in)
  - `vm_memory_high_watermark_paging_ratio = 0.5` (page messages to disk at 54MB)
  - Disable the management UI plugin in production to save ~30MB — or accept the cost for debugging convenience
  - `disk_free_limit.relative = 1.5`

#### Step 30: Write the Production Docker Compose

- Create `docker-compose.prod.yml` with `mem_limit` and `memswap_limit` on every container:
  ```yaml
  services:
    auth-service:
      image: nexusvas/auth-service:${TAG}
      mem_limit: 192m
      memswap_limit: 256m    # 64MB swap allowed
      environment:
        JAVA_OPTS: "-Xms64m -Xmx128m -XX:+UseG1GC ..."
      restart: unless-stopped
      depends_on:
        postgres:
          condition: service_healthy
        redis:
          condition: service_healthy
      healthcheck:
        test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
        interval: 30s
        timeout: 5s
        retries: 3
        start_period: 60s
  ```
- Set `restart: unless-stopped` on all containers — if a service OOMs, Docker restarts it automatically
- Use `start_period: 60s` on health checks — Spring Boot needs time to start on a constrained JVM
- Define `depends_on` with `condition: service_healthy` to enforce startup order: infrastructure first → Auth Service → Gateway → remaining services
- Use Docker's `logging` driver with `max-size: 10m` and `max-file: 3` to prevent log files from consuming disk
- **Network**: All services on a single Docker bridge network — no overlay networking overhead

#### Step 31: Set Up 2GB Swap

- Swap is a safety net — it prevents hard OOM kills when traffic spikes cause temporary memory pressure:
  ```bash
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  ```
- Set `vm.swappiness=10` — Linux will prefer RAM and only swap when memory pressure is high
- Swap means a service that briefly exceeds its heap limit will slow down (disk I/O) instead of getting killed
- Monitor swap usage in Grafana — if swap is consistently used, a service needs memory limit adjustment

#### Step 32: Staggered Startup Script

- With 6GB, starting all containers simultaneously causes a memory spike as 10 JVMs initialize concurrently, each loading Spring contexts, scanning classpaths, and running Flyway migrations
- Write a `start.sh` script that starts services in waves with health check gates:
  - **Wave 1**: PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ (infrastructure)
  - Wait for all infrastructure health checks to pass
  - **Wave 2**: Auth Service (must be up before others can validate tokens)
  - Wait for Auth Service health check
  - **Wave 3**: API Gateway, Operator Service, Subscription Service
  - **Wave 4**: Billing Service, Content Service, Campaign Service
  - **Wave 5**: Notification Service, Analytics Service, AI Service
  - **Wave 6**: Prometheus, Grafana
- This reduces peak concurrent memory usage during startup by ~40%

#### Step 33: Service Consolidation Option (If RAM Is Still Tight)

If after tuning you're still hitting memory pressure, consolidate low-traffic services into a single deployable:

- **Merge candidates**: Notification Service + Campaign Service → `campaign-notification-service` (they're closely coupled anyway — Campaign dispatches, Notification delivers). Saves ~160MB.
- **Merge candidates**: Operator Service + Auth Service → `identity-service` (Operator onboarding is tightly coupled with auth setup). Saves ~160MB.
- **Keep separate**: Subscription Service, Billing Service, Content Service, Analytics Service, AI Service — these have distinct scaling needs and should remain independent.
- After consolidation: 7 services instead of 10, saving ~320MB. Total application memory drops from ~1800MB to ~1480MB.
- **Important**: Even if deployed as a single JAR, keep the code modular (separate packages/modules). Consolidation is a deployment optimization, not an architectural compromise.

#### Step 34: CI/CD with GitHub Actions (Build Remote, Deploy to VPS)

- **CI Pipeline** (on every PR — runs on GitHub Actions runners, NOT your VPS):
  - Checkout code
  - Run Flyway validation with Testcontainers
  - Run unit tests (`mvn test`)
  - Run integration tests with Testcontainers
  - Run Swagger contract validation (`openapi-diff`)
  - Run static analysis (SpotBugs, Checkstyle)
  - Build Docker images
  - Push images to Docker Hub or GitHub Container Registry
- **CD Pipeline** (on merge to main):
  - SSH into VPS via GitHub Actions (`appleboy/ssh-action`)
  - Pull latest images: `docker compose -f docker-compose.prod.yml pull`
  - Rolling restart: stop and restart services one at a time (not all at once) to maintain availability
  - Run smoke tests against the live endpoints
  - If smoke tests fail, rollback: `docker compose -f docker-compose.prod.yml up -d --no-deps {service} --tag {previous-tag}`
- **Never build images on the VPS** — the Maven build + Docker build process needs 2+ GB of RAM itself. Build on GitHub Actions (14GB runners), push to registry, pull on VPS.

#### Step 35: VPS Monitoring & Alerts

- **Prometheus** (128MB limit): scrapes all services' `/actuator/prometheus` endpoints every 15s (not 5s — less CPU/memory on tight hardware)
- **Grafana** (128MB limit): dashboards for system and business metrics
- **Critical alerts** (via Grafana → webhook to your phone/Slack):
  - Container restart count > 2 in 5 minutes (OOM kills)
  - Host RAM usage > 85%
  - Swap usage > 512MB (sustained)
  - Disk usage > 80%
  - Any service health check failing for > 2 minutes
  - Kafka consumer lag > 5,000
  - PostgreSQL connection pool exhaustion
- **Disk management**: VPS storage is finite
  - Docker log rotation: `max-size: 10m`, `max-file: 3` per container
  - Kafka log retention: 7 days max
  - MongoDB: TTL indexes auto-expire old audit logs (90 days)
  - PostgreSQL: Partition old event store data and DETACH/DROP old partitions quarterly
  - Cron job: `docker system prune -f` weekly to reclaim unused images/layers

#### Step 36: Final Production Checklist

- All secrets in a `.env` file with `chmod 600`, referenced in Docker Compose via `env_file`
- Database migrations managed with Flyway (see Phase 1B) — `ddl-auto=validate`, `clean-disabled=true`
- API documentation live at `https://your-domain/swagger-ui.html` via API Gateway aggregated Swagger
- GraphQL schema published at `https://your-domain/analytics/graphiql`
- Circuit breakers (Resilience4j) on all inter-service HTTP calls
- Graceful shutdown on all services (`server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=15s`)
- UFW firewall: only ports 80, 443, and your SSH port open
- Nginx or Caddy as reverse proxy in front of API Gateway — handles TLS termination, HTTP/2, and static file serving
- 2GB swap enabled with `swappiness=10`
- Staggered startup script tested and working
- Automated backups: PostgreSQL `pg_dump` + MongoDB `mongodump` nightly to object storage (S3, Backblaze B2)
- README with architecture diagram, local setup instructions, VPS deployment guide, and memory budget table

---

### Phase 11 — Kubernetes Deployment (Cloud-Ready Showcase)

The 6GB VPS runs Docker Compose for actual production. This phase exists in parallel — a complete set of Kubernetes manifests and Helm charts checked into the repository under `/k8s/`. It demonstrates that the architecture is cloud-ready and that you understand K8s orchestration, even though your current deployment target is a single VPS. In an interview, you can walk through these manifests and explain every decision. If MIAKI later deploys to AWS EKS, GCP GKE, or Azure AKS for a client, the K8s config is production-ready.

#### Kubernetes Directory Structure

- Organize the K8s configuration in the repository:
  ```
  k8s/
  ├── helm/
  │   └── nexusvas/
  │       ├── Chart.yaml
  │       ├── values.yaml                  (defaults)
  │       ├── values-dev.yaml
  │       ├── values-staging.yaml
  │       ├── values-prod.yaml
  │       └── templates/
  │           ├── _helpers.tpl
  │           ├── namespace.yaml
  │           ├── auth-service/
  │           │   ├── deployment.yaml
  │           │   ├── service.yaml
  │           │   ├── configmap.yaml
  │           │   ├── secret.yaml
  │           │   └── hpa.yaml
  │           ├── operator-service/
  │           ├── subscription-service/
  │           ├── billing-service/
  │           ├── content-service/
  │           ├── campaign-service/
  │           ├── notification-service/
  │           ├── analytics-service/
  │           ├── ai-service/
  │           ├── api-gateway/
  │           │   ├── deployment.yaml
  │           │   ├── service.yaml
  │           │   └── ingress.yaml
  │           └── infrastructure/
  │               ├── postgres-statefulset.yaml
  │               ├── mongodb-statefulset.yaml
  │               ├── redis-statefulset.yaml
  │               ├── rabbitmq-statefulset.yaml
  │               └── kafka/              (Strimzi KRaft CRDs — no Zookeeper)
  ├── operators/
  │   ├── strimzi-kafka-operator.yaml
  │   └── rabbitmq-cluster-operator.yaml
  └── README.md
  ```

#### Write Deployment Manifests for Each Microservice

- For each of the 10 application services, create a `Deployment` manifest:
  - `replicas: 2` (minimum for high availability — the VPS runs 1 replica, K8s runs 2+)
  - Container `resources`:
    ```yaml
    resources:
      requests:
        memory: "192Mi"
        cpu: "100m"
      limits:
        memory: "384Mi"
        cpu: "500m"
    ```
  - `readinessProbe` pointing to Spring Actuator: `httpGet /actuator/health/readiness` with `initialDelaySeconds: 30`, `periodSeconds: 10`
  - `livelinessProbe` pointing to: `httpGet /actuator/health/liveness` with `initialDelaySeconds: 60`, `periodSeconds: 15`, `failureThreshold: 3`
  - `env` variables injected from `ConfigMap` (non-sensitive) and `Secret` (credentials, API keys, JWT signing keys)
  - `JAVA_OPTS` set via `env` — same tuning flags as Docker Compose but with higher heap (`-Xmx256m`) since K8s clusters have more RAM
  - `imagePullPolicy: IfNotPresent` for staging, `Always` for production
  - Pod anti-affinity rules: spread replicas of the same service across different nodes for resilience
- For each service, create a `Service` manifest:
  - `type: ClusterIP` for all internal services
  - `type: LoadBalancer` only for the API Gateway (or use Ingress instead)
  - Named ports matching Spring Boot's `server.port`

#### Write ConfigMap and Secret for Each Service

- **ConfigMap** per service holds:
  - Spring profile: `SPRING_PROFILES_ACTIVE: prod`
  - Database URLs: `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-svc:5432/auth_db`
  - Kafka bootstrap: `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-svc:9092`
  - Redis host: `SPRING_DATA_REDIS_HOST: redis-svc`
  - RabbitMQ host: `SPRING_RABBITMQ_HOST: rabbitmq-svc`
  - MongoDB URI: `SPRING_DATA_MONGODB_URI: mongodb://mongodb-svc:27017/content_db`
  - Service-specific configs (rate limit tiers, feature flags)
- **Secret** per service holds (base64-encoded):
  - Database passwords
  - JWT signing keys (RSA private key for Auth Service, public key for Resource Servers)
  - OpenRouter API key (for AI Service)
  - RabbitMQ/Kafka credentials
- Use `kubectl create secret generic` or seal secrets with Sealed Secrets / External Secrets Operator for GitOps compatibility

#### Write HorizontalPodAutoscaler (HPA) for Each Service

- Define autoscaling policies per service based on their traffic patterns:
  - **API Gateway**: scale on CPU (target 60%) — it handles all incoming traffic
    ```yaml
    minReplicas: 2
    maxReplicas: 8
    metrics:
      - type: Resource
        resource:
          name: cpu
          target:
            type: Utilization
            averageUtilization: 60
    ```
  - **Content Service**: scale on CPU (target 50%) — highest throughput endpoint, needs aggressive scaling
    ```yaml
    minReplicas: 2
    maxReplicas: 10
    ```
  - **Auth Service**: scale on CPU (target 70%) — token validation is lightweight, doesn't need aggressive scaling
    ```yaml
    minReplicas: 2
    maxReplicas: 4
    ```
  - **Subscription Service / Billing Service**: scale on both CPU and custom Kafka consumer lag metric (via Prometheus Adapter)
    ```yaml
    minReplicas: 2
    maxReplicas: 6
    metrics:
      - type: Resource
        resource: { name: cpu, target: { averageUtilization: 65 } }
      - type: Pods
        pods:
          metric: { name: kafka_consumer_lag }
          target: { type: AverageValue, averageValue: "1000" }
    ```
  - **Notification Service / Campaign Service**: scale on RabbitMQ queue depth (custom metric)
  - **Analytics Service / AI Service**: `minReplicas: 1`, `maxReplicas: 3` — lower traffic, scale conservatively

#### Write Infrastructure StatefulSets

- **PostgreSQL**:
  - Use a `StatefulSet` with 1 replica (single-primary) or use a managed database (RDS, Cloud SQL) in real cloud deployments
  - `PersistentVolumeClaim`: 20Gi with `ReadWriteOnce` access mode
  - Init container runs the `init.sql` to create all 7 databases
  - ConfigMap for `postgresql.conf` overrides: `shared_buffers`, `max_connections`, `work_mem` (scaled up from VPS values since K8s nodes have more RAM)
  - Readiness probe: `exec pg_isready`
- **MongoDB**:
  - `StatefulSet` with 1 replica, PVC 10Gi
  - ConfigMap for `mongod.conf`: WiredTiger cache sized to 25% of the container's memory limit
- **Redis**:
  - `StatefulSet` with 1 replica, PVC 2Gi
  - ConfigMap for `redis.conf`: `maxmemory`, `maxmemory-policy`, persistence settings
  - Or use Redis Sentinel / Redis Cluster for HA (3 replicas)
- **Kafka** (via Strimzi Operator — KRaft mode, no Zookeeper):
  - Install Strimzi Kafka Operator (v0.38+) in the cluster — this version supports KRaft natively
  - **Why KRaft in K8s too**: Consistency with the Docker Compose deployment (both run KRaft), eliminates the Zookeeper StatefulSet (saves 3 pods × ~256MB each = ~768MB cluster-wide), faster controller failover, and Zookeeper support is removed from Kafka 4.0 anyway — there's no reason to use a deprecated component in a new project
  - Define a `KafkaNodePool` and `Kafka` Custom Resource with KRaft enabled:
    ```yaml
    apiVersion: kafka.strimzi.io/v1beta2
    kind: KafkaNodePool
    metadata:
      name: nexusvas-brokers
      labels:
        strimzi.io/cluster: nexusvas-kafka
    spec:
      replicas: 3
      roles:
        - controller
        - broker
      storage:
        type: persistent-claim
        size: 20Gi
      resources:
        requests:
          memory: "512Mi"
          cpu: "250m"
        limits:
          memory: "1Gi"
          cpu: "1000m"
    ---
    apiVersion: kafka.strimzi.io/v1beta2
    kind: Kafka
    metadata:
      name: nexusvas-kafka
      annotations:
        strimzi.io/kraft: enabled
        strimzi.io/node-pools: enabled
    spec:
      kafka:
        version: "3.8.0"
        metadataVersion: "3.8-IV0"
        listeners:
          - name: plain
            port: 9092
            type: internal
            tls: false
          - name: tls
            port: 9093
            type: internal
            tls: true
        config:
          num.partitions: 6
          default.replication.factor: 3
          min.insync.replicas: 2
          log.retention.hours: 168
          offsets.topic.replication.factor: 3
          transaction.state.log.replication.factor: 3
          transaction.state.log.min.isr: 2
      entityOperator:
        topicOperator: {}
    ```
  - Note: No `spec.zookeeper` section at all — Strimzi with the `strimzi.io/kraft: enabled` annotation manages the entire cluster using KRaft consensus. The `KafkaNodePool` CRD defines nodes that serve both `controller` and `broker` roles (combined mode), which is simpler for a 3-node cluster. For larger deployments, separate the roles into dedicated controller and broker node pools.
  - Define `KafkaTopic` CRDs for each topic: `subscription-events`, `billing-events`, `content-events`, `campaign-events` — with partition count and replication factor
- **RabbitMQ** (via RabbitMQ Cluster Operator):
  - Install the RabbitMQ Cluster Operator
  - Define a `RabbitmqCluster` Custom Resource with 3 replicas, persistent storage, and resource limits

#### Write Ingress and TLS Configuration

- Create an `Ingress` resource for the API Gateway:
  ```yaml
  apiVersion: networking.k8s.io/v1
  kind: Ingress
  metadata:
    name: nexusvas-ingress
    annotations:
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
      cert-manager.io/cluster-issuer: "letsencrypt-prod"
  spec:
    ingressClassName: nginx
    tls:
      - hosts:
          - api.nexusvas.com
        secretName: nexusvas-tls
    rules:
      - host: api.nexusvas.com
        http:
          paths:
            - path: /
              pathType: Prefix
              backend:
                service:
                  name: api-gateway-svc
                  port:
                    number: 8080
  ```
- Install **cert-manager** for automatic Let's Encrypt TLS certificates
- Install **NGINX Ingress Controller** (or Traefik) as the cluster's ingress provider
- Path-based routing for Swagger docs: `/swagger-ui.html` → API Gateway's aggregated Swagger

#### Write NetworkPolicies for Service Isolation

- Define Kubernetes `NetworkPolicy` resources to enforce microservice communication boundaries:
  - **Auth Service**: accepts ingress from API Gateway and all application services (for token validation). No direct access from outside the cluster.
  - **Subscription Service**: accepts ingress from API Gateway only. Egress to PostgreSQL, Kafka, Redis, Auth Service, Billing Service.
  - **Billing Service**: accepts ingress from API Gateway and Subscription Service (for Saga callbacks). Egress to PostgreSQL, Kafka, Redis, Auth Service.
  - **Content Service**: accepts ingress from API Gateway only. Egress to MongoDB, Redis, Kafka, Auth Service, AI Service (for recommendations).
  - **AI Service**: accepts ingress from Content Service, Campaign Service, Billing Service (internal only — not exposed via Gateway in prod). Egress to PostgreSQL (PGVector), Kafka, Redis, OpenRouter API (external HTTPS).
  - **Infrastructure pods** (PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ): accept ingress only from application service namespaces — no external access.
- This demonstrates defense-in-depth security at the network layer

#### Helm Values Per Environment

- `values-dev.yaml`:
  - All services: `replicas: 1`, lower resource limits, debug logging enabled
  - PostgreSQL/MongoDB: small PVCs (5Gi)
  - Kafka: single KRaft node (combined controller + broker role), single partition
  - HPA disabled
  - Zipkin enabled for tracing
- `values-staging.yaml`:
  - All services: `replicas: 2`, production-like resource limits
  - Full HPA enabled with conservative thresholds
  - Kafka: 3 KRaft nodes (combined roles), 3 partitions
  - Prometheus + Grafana enabled
- `values-prod.yaml`:
  - All services: `replicas: 2` minimum, aggressive HPA
  - Kafka: 3 KRaft nodes (combined roles), 6 partitions, replication factor 3, `min.insync.replicas: 2`
  - PostgreSQL: consider managed (RDS) for automated backups and failover
  - Pod Disruption Budgets: `minAvailable: 1` on all services — guarantees at least 1 pod survives during rolling updates or node drains
  - Resource limits tuned per service based on load test results

#### CI/CD Kubernetes Pipeline (Extends Phase 10 GitHub Actions)

- The CI pipeline remains the same (build, test, push images)
- Add a **K8s CD pipeline** triggered on tag push (e.g., `v1.2.0`):
  - Authenticate to the K8s cluster using a service account token stored in GitHub Secrets
  - Run `helm upgrade --install nexusvas ./k8s/helm/nexusvas -f values-prod.yaml --set image.tag=${TAG}`
  - Wait for rollout: `kubectl rollout status deployment/{service} --timeout=300s` for each service
  - Run integration smoke tests against the K8s Ingress endpoint
  - If any rollout fails: `helm rollback nexusvas` to the previous release
- **GitOps alternative**: Install ArgoCD in the cluster and point it at the `k8s/helm/` directory in the Git repo. Every merge to `main` triggers ArgoCD to sync the desired state to the cluster automatically.

---

### Phase 12 — AI Service with Spring AI

This phase adds an intelligent layer to the platform. Instead of a standalone ML project, the AI Service is deeply integrated into the existing event-driven architecture — it consumes domain events, enriches data, and exposes AI capabilities as internal APIs that other services call.

#### Step 37: Set Up the AI Service Module

- Create the `ai-service` Spring Boot module
- Add Spring AI dependency: `spring-ai-openai-spring-boot-starter` — OpenRouter is compatible with the OpenAI API format, so Spring AI's OpenAI client works directly with OpenRouter by overriding the base URL
- Configure OpenRouter in `application.yml`:
  - `spring.ai.openai.base-url: https://openrouter.ai/api/v1`
  - `spring.ai.openai.api-key: ${OPENROUTER_API_KEY}` (injected from `.env` file via Docker Compose)
  - `spring.ai.openai.chat.options.model: google/gemini-2.5-flash` (default model — configurable per tenant or per use case)
  - Add custom headers required by OpenRouter: `HTTP-Referer` (your app URL) and `X-Title` (app name) via a `RestClient` customizer bean
- Configure model selection per use case via application properties:
  - `app.ai.models.churn-scoring: google/gemini-2.5-flash` (fast, cost-effective for batch scoring)
  - `app.ai.models.rag-chat: anthropic/claude-sonnet-4` (high quality for subscriber support)
  - `app.ai.models.campaign-copy: openai/gpt-4o` (creative writing)
  - `app.ai.models.fraud-detection: google/gemini-2.5-flash` (fast reasoning with function calling)
  - `app.ai.models.embedding: openai/text-embedding-3-small` (embeddings for RAG)
- Build a `ModelRouter` bean that selects the correct `ChatClient` configuration based on the use case — this lets you mix models from different providers (Google, Anthropic, OpenAI) through a single OpenRouter API key
- Add PGVector dependency (`spring-ai-pgvector-store-spring-boot-starter`) for the vector store
- Configure a dedicated PostgreSQL database for the AI Service with the `pgvector` extension enabled
- Wire up the `ChatClient`, `EmbeddingModel`, and `VectorStore` beans

#### Step 38: Build the RAG-Powered Knowledge Base (Subscriber Support)

- **Purpose**: Operator admin teams upload product documentation, FAQs, troubleshooting guides, and pricing plans. The AI Service enables natural language Q&A over this content — powering a subscriber support chatbot or an internal operator help desk.
- **Document Ingestion Pipeline**:
  - Build a REST endpoint that accepts document uploads (PDF, Markdown, plain text) scoped to a `tenantId`
  - Use Spring AI's `DocumentReader` (PDF reader, text reader) to extract content
  - Split documents into chunks using `TokenTextSplitter` (512 tokens per chunk with 50-token overlap)
  - Generate embeddings for each chunk via the configured `EmbeddingModel`
  - Store chunks + embeddings in PGVector with metadata: `tenantId`, `documentId`, `chunkIndex`, `source`
- **Query Pipeline (RAG)**:
  - Build a REST endpoint: `POST /api/v1/ai/ask` — accepts a natural language question + `tenantId`
  - Perform similarity search on PGVector filtered by `tenantId` — retrieve top 5 relevant chunks
  - Construct a prompt using Spring AI's `PromptTemplate`: system instruction (you are a telecom VAS support assistant) + retrieved context chunks + user question
  - Call the `ChatClient` and return the structured response
  - Use Spring AI's `OutputParser` with `BeanOutputConverter` to return structured JSON (answer, confidence score, source document references)
- **Tenant Isolation**: Ensure vector search is always filtered by `tenantId` — Operator A's documents must never leak into Operator B's responses

#### Step 39: Build Churn Prediction Scoring

- **Purpose**: Identify subscribers at high risk of cancellation so the Campaign Service can target them with retention offers.
- **Event Consumer**:
  - Create a Kafka consumer that listens to `subscription-events`, `billing-events`, and `content-events`
  - For each subscriber, maintain a feature profile in PostgreSQL: days since last content access, billing failure count, subscription age, renewal count, content delivery count, time since last activity
- **Scoring Endpoint**:
  - Build an internal REST endpoint: `POST /api/v1/ai/churn-score` — accepts subscriber features
  - Construct a structured prompt using Spring AI's `PromptTemplate`: provide the subscriber's behavioral features and ask the LLM to assess churn risk as a JSON object with `riskScore (0-100)`, `riskLevel (LOW/MEDIUM/HIGH/CRITICAL)`, `topFactors[]`, and `recommendedAction`
  - Use `BeanOutputConverter` to parse the LLM response into a strongly-typed Java `ChurnAssessment` record
- **Batch Scoring Job**:
  - Build a Spring `@Scheduled` job that runs nightly
  - Queries all active subscribers, scores each one, and stores results in a `churn_scores` table
  - Publishes `ChurnRiskScored` events to Kafka for subscribers above the HIGH threshold
  - The Campaign Service consumes these events and auto-creates retention campaigns

#### Step 40: Build Smart Content Recommendations

- **Purpose**: Given a subscriber's history (what they've consumed, their subscription type, their demographic), recommend the next best content items.
- **Approach (Embedding-Based Similarity + LLM Re-ranking)**:
  - When content is published, generate an embedding of its metadata (title + description + tags) and store in PGVector
  - When a recommendation is requested, build the subscriber's preference profile from their last 20 `ContentDelivered` events
  - Perform a PGVector similarity search: find content whose embeddings are closest to the subscriber's preference centroid, filtered by `tenantId`
  - Pass the top 15 candidates to the LLM with the subscriber's profile and ask it to re-rank and select the best 5 with explanations
  - Return recommendations as structured JSON using `BeanOutputConverter`
- **Expose as REST API**: `GET /api/v1/ai/recommendations/{msisdn}` — called by the Content Service to personalize delivery
- **Cache recommendations in Redis** with a TTL of 6 hours to avoid repeated LLM calls for the same subscriber

#### Step 41: AI-Powered Campaign Copy Generation

- **Purpose**: When operators create a new campaign in the Campaign Service, the AI Service generates optimized SMS/USSD copy.
- **Flow**:
  - Campaign Service calls `POST /api/v1/ai/campaign-copy` with: product details, target audience description, campaign goal (acquisition/retention/upsell), channel (SMS/USSD), max character limit
  - AI Service constructs a prompt with constraints: character limit, telecom tone, include call-to-action, include short-code keyword
  - Use Spring AI's `ChatClient` with a system prompt tuned for telecom marketing
  - Generate 3 variants with different tones (urgent, friendly, informational)
  - Return structured response with `variants[]`, each containing `text`, `tone`, `characterCount`
  - Operator selects their preferred variant in the Campaign Service UI

#### Step 42: Billing Fraud Detection

- **Purpose**: Flag suspicious billing patterns — e.g., a single MSISDN being charged for 50 different products in 1 minute, or unusual geographic patterns.
- **Event Consumer**:
  - Create a Kafka consumer that listens to `billing-events` in real-time
  - For each billing event, pull the subscriber's recent billing history from Redis/PostgreSQL (last 24 hours)
- **Anomaly Detection via Function Calling**:
  - Use Spring AI's Function Calling feature to define tools the LLM can invoke:
    - `getSubscriberBillingHistory(msisdn, hours)` — returns recent charges
    - `getSubscriberProfile(msisdn)` — returns account age, typical usage patterns
    - `flagForReview(msisdn, reason, severity)` — marks the subscriber for manual review
  - Send the billing event + context to the LLM with tool definitions
  - The LLM reasons about the pattern and decides whether to call `flagForReview`
  - If flagged, publish a `FraudFlagged` event to Kafka — the Billing Service can pause future charges pending review
- **Fallback**: For high-throughput, implement simple rule-based pre-filters (e.g., >10 charges in 5 minutes) before invoking the LLM — only escalate edge cases to the AI

#### Step 43: AI Observability & Cost Management

- **Prompt Logging**: Log every LLM call to MongoDB — `requestId`, `tenantId`, `promptTemplate`, `tokenCountInput`, `tokenCountOutput`, `latencyMs`, `model`, `openRouterGenerationId`, `cost`
- **OpenRouter Cost Tracking**: After each API call, read the `x-openrouter-generation-id` response header and query `https://openrouter.ai/api/v1/generation?id={id}` to get exact cost breakdown (prompt tokens, completion tokens, cost per model). Store per-tenant cost records in PostgreSQL for monthly invoicing.
- **Token Budget per Tenant**: Store each operator's monthly AI token budget in Redis. Before every LLM call, check remaining budget. If exhausted, return a fallback response or queue for next billing cycle. Budget is tracked per model tier — cheap models (Gemini Flash) have separate limits from expensive models (Claude Sonnet).
- **Model Fallback Chain**: If the primary model (e.g., `anthropic/claude-sonnet-4`) returns a rate limit or error from OpenRouter, automatically fall back to the next model in the chain (e.g., `google/gemini-2.5-flash`). Implement this in the `ModelRouter` bean with Resilience4j circuit breakers per model.
- **Metrics**: Expose Micrometer metrics for AI-specific counters:
  - `ai.llm.calls.total` (tagged by endpoint, model, tenant, provider)
  - `ai.llm.tokens.consumed` (input/output breakdown, tagged by model)
  - `ai.llm.latency` (histogram, tagged by model)
  - `ai.llm.cost.usd` (counter, tagged by tenant and model)
  - `ai.llm.fallbacks.total` (counter for model fallback events)
  - `ai.rag.retrieval.relevance_score` (average similarity score of retrieved chunks)
- **Grafana Dashboard**: Add an AI panel to the existing monitoring dashboard — token consumption trends, cost per tenant per model, latency percentiles by model, churn score distribution, OpenRouter rate limit hit frequency

---

## What This Project Demonstrates to MIAKI

| What They Want | What You Show |
|---|---|
| Scalable backend systems | Multi-tenant microservices handling millions of events/day |
| Spring Boot + entire ecosystem | Every Spring module used with purpose — Spring AI, Spring Authorization Server, Spring WebFlux, Spring Data JPA, Spring Cloud Gateway |
| Kafka + RabbitMQ | Kafka for event streaming backbone, RabbitMQ for task dispatch with DLQ |
| PostgreSQL + MongoDB + Redis | Each used for its ideal purpose — transactional, document, caching |
| Event Sourcing / CQRS | Subscription lifecycle as immutable event stream with read projections |
| REST + GraphQL + WebFlux | REST for CRUD, GraphQL for analytics, WebFlux for high-throughput content delivery and reactive streaming |
| Docker + CI/CD (VPS) | Production Docker Compose on 6GB VPS — memory-budgeted containers, JVM tuning, staggered startup, GitHub Actions with remote build + SSH deploy |
| Kubernetes (Cloud-Ready) | Complete Helm charts, HPA per service, Strimzi Kafka, RabbitMQ Operator, NetworkPolicies, Ingress + TLS, multi-environment values files, GitOps-ready |
| OAuth2 + JWT + API Security | Dedicated Auth Service — Spring Authorization Server, RBAC policy engine, 4 auth flows (API key, portal login + MFA, subscriber OTP, service-to-service), token revocation, JWKS, audit logging |
| API Documentation (Swagger) | SpringDoc OpenAPI on every service, aggregated Swagger UI via Gateway, security schemes, API versioning, contract validation in CI, client SDK generation |
| Observability | Log-based distributed tracing, Prometheus + Grafana metrics, Loki for lightweight log aggregation — all tuned for 6GB VPS |
| FinTech / Telecom domain | Double-entry billing ledger, telecom VAS subscription lifecycle, CDN-integrated content delivery, offline content packs |
| Database migration (Flyway) | Per-service migration ownership (7 databases), partitioning scripts, CI validation, zero-downtime strategy |
| Spring AI + OpenRouter | RAG knowledge base, churn prediction, recommendations, fraud detection, campaign copy — multi-model routing (Gemini, Claude, GPT) through single OpenRouter API |
| Resource-constrained deployment | Complete 6GB VPS memory budget, JVM tuning (-Xmx128m), infra tuning (Kafka KRaft 256m, PG shared_buffers 96MB, Mongo wiredTiger 100MB), swap, service consolidation strategy |
| AI tools for productivity (JD behavioral expectation) | AI is not a bolt-on demo — it's woven into the event-driven architecture as a first-class service |
| High ownership & systems thinking | End-to-end architecture — from domain events to K8s NetworkPolicies to VPS swap configuration |