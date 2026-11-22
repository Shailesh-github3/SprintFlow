# SprintFlow — Project & Issue Tracker

A backend-focused full-stack project-management platform: users register, create and manage projects, invite team members by email, track issues across statuses and priorities, assign tasks within project boundaries, discuss via comments and per-project chat, monitor live project audit feeds, and manage FREE/MONTHLY/YEARLY subscriptions with verified Razorpay payment workflows.

**Stack (what's actually used):** Java 21 · Spring Boot 3.5 (Web, Security, Data JPA, Validation, Mail, Actuator) · MySQL 8 + Flyway · JWT in HttpOnly cookies · MapStruct · springdoc-openapi · React 19 + Vite · React Router 7 · TanStack Query 5 · react-hook-form + Zod · Radix/shadcn + Tailwind · Axios · Recharts · Sonner · Testcontainers · Vitest. No Redux, no microservices, no Kafka, no AI — an intentionally engineered modular monolith.

---

## Architecture

```
Browser (React/Vite) ──HTTPS/JSON + HttpOnly cookie──> Spring Boot API ──JPA──> MySQL 8
                        Swagger UI at /swagger-ui.html   Flyway migrations ──> (V1 schema, V2 constraints, V3 audit_log)
                        Actuator at /actuator/health     SMTP (Gmail) ──> Async invitation emails
                                                         Razorpay ──> Payment links & HMAC-SHA256 verification
```

Layered monolith architecture: `controller → service → repository → entity`, MapStruct DTOs at the API boundary, imperative ownership/membership checks (`ProjectService.ensureProjectOwner / ensureProjectMemberOrOwner`), global `ApiErrorResponse` standard error shape, and transactional service boundaries.

---

## Features 

| Feature | Status | Implementation Details |
|---|---|---|
| Register / login / logout | Working | BCrypt password hashing + stateless JWT in HttpOnly `SameSite=Lax` cookies |
| Project CRUD, filter & search | Working | Paged listing with category & tag filters, debounced server-side search |
| Domain Quota Enforcement | Working | Free tier users limited to 3 active projects; enforced in `ProjectService` with 403 response |
| Email Invitations | Working | Non-blocking `@Async` SMTP delivery, 7-day expiration, atomic one-time accept flow |
| Issue Tracking & Assignment | Working | Paged issue lists, status transitions (TODO/IN_PROGRESS/DONE), priority badges |
| Owner-Gated Issue Assignment | Working | Strictly validated project-membership assignment & unassign endpoint with audit trails |
| Issue Comments | Working | Member-only read/write, author-only deletion, project boundary enforcement |
| Per-Project Chat | Working | Polling-based live chat (5s interval, chronological ordering) with auto-scroll |
| Project Activity Feed | Working | Real-time append-only audit feed (`V3__audit_log`) rendered with action badges & summaries |
| Subscriptions & Upgrades | Working | FREE / MONTHLY / YEARLY tiers with HMAC-SHA256 signature-verified Razorpay completion |
| Observability & Health Checks | Working | Spring Boot Actuator exposing `/actuator/health`, `/actuator/info`, and `/actuator/metrics` |
| Dashboard & Profile Settings | Working | Real-time project & task stats, user profile hydration, dark/light theme toggle |

---

## Performance & Database Engineering

- **Elimination of In-Memory Pagination (`HHH000104`)**:
  - Replaced eager collection joins in `@EntityGraph` for paginated project and issue queries with to-one associations (`projectOwner`, `chat`, `assignedUser`).
  - Ensures SQL execution leverages native database `LIMIT` and `OFFSET` rather than loading entire association trees into JVM memory.
- **Elimination of N+1 Queries via Batch Fetching**:
  - Configured `spring.jpa.properties.hibernate.default_batch_fetch_size=20` in `application.properties`.
  - Added `@BatchSize(size = 20)` to `Project.tags`, `Project.projectMembers`, and `Issue.tags`.
  - Fetching child collections across paginated responses runs single `IN (?, ?...)` queries across the page rather than N individual roundtrips.
- **Flyway Database Migrations**:
  - `V1__initial_schema.sql`: Full normalized relational schema (users, roles, projects, members, issues, comments, chats, messages, invitations, subscriptions) with InnoDB/`utf8mb4` and foreign key constraints.
  - `V2__issue_constraints.sql`: Database-level `CHECK` constraints locking issue statuses and priorities, plus indexes on `invitation.token` and `message.chat_id`.
  - `V3__audit_log.sql`: Append-only audit table with composite index `(project_id, timestamp)` for high-throughput activity queries.
  - `spring.jpa.hibernate.ddl-auto=validate`: Hibernate never alters the database schema directly; Flyway owns all migrations.

---

## Security Architecture

- **Authentication & Cookies**:
  - Stateless JWT tokens stored in HttpOnly cookies with `SameSite=Lax` (and `Authorization: Bearer` support for CLI/API clients).
  - Rate limiting via `RateLimitFilter`: in-memory sliding window (20 requests/minute/IP on `/api/auth/**`) returning HTTP 429.
  - Form validation powered by Spring Validation (backend) and Zod + React Hook Form (frontend) with field-level feedback.
- **Multi-Tenancy & Authorization Boundaries**:
  - Every `/api/**` endpoint is protected by Spring Security method authorization (`@PreAuthorize("isAuthenticated()")`).
  - Access to project data, issues, comments, chat, and activity logs requires explicit ownership or membership checks in the service layer (`ProjectService.ensureProjectMemberOrOwner`).
  - Cross-project invitation collisions eliminated via multi-tenancy query `findByUserEmailAndProjectId`.
- **Payment Integrity**:
  - Direct plan upgrade endpoints removed; plan upgrades require authenticated requests to `POST /api/payments/verify` with Razorpay transaction parameters and HMAC-SHA256 signature verification.

---

## Quick Start

### Prerequisites
- Docker & Docker Compose (for running MySQL + Spring Boot API together)
- Java 21 & Maven 3.9+ (for local backend development)
- Node.js 22+ & npm (for frontend development)

### 1. Environment Setup
```bash
# Copy example environment files (never commit real .env files)
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env

# In backend/.env:
# Configure SPRINGFLOW_JWT_SECRET (e.g. openssl rand -hex 32)
# Configure MySQL credentials and optional SMTP/Razorpay keys
```

### 2. Run via Docker Compose
```bash
docker compose up --build
```
- App UI: http://localhost:5173
- Spring Boot API: http://localhost:8080
- Swagger / OpenAPI Docs: http://localhost:8080/swagger-ui.html
- Actuator Health: http://localhost:8080/actuator/health

### 3. Local Development (Independent Services)
```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend
cd frontend
npm install
npm run dev
```

---

## Testing & Quality Assurance

```bash
# Backend verification (81 unit & integration tests)
cd backend && mvn test

# Frontend validation (vitest + oxlint + production build)
cd frontend
npm test
npm run lint
npm run build
```

- **Backend (81 Tests, 100% Pass Rate)**:
  - Controller authorization tests: 401 unauthenticated, 403 forbidden, 409 conflict, 429 rate limit, IDOR prevention.
  - Service unit tests: `ProjectServiceUnitTest` (ownership, quota boundaries), `IssueServiceAuditTest` (assignment, audit trails), `PaymentServiceTest` (signature verification), `RateLimitFilterTest`, `AuditServiceTest`.
  - Repository integration tests: Verified with Testcontainers MySQL (gracefully skipped if Docker is absent).
- **Frontend (5 Vitest Tests)**:
  - Form validations (short title, malformed email, enum types), dialog triggers, authentication flows.
  - Oxlint linter: 0 errors.
- **Continuous Integration (`.github/workflows/ci.yml`)**:
  - Automates backend `mvn -B verify` against a MySQL 8.0 service container.
  - Automates frontend `npm ci`, `npm run lint`, `npm test`, and `npm run build` on every push and pull request.

---

## Deliberate Design Trade-Offs

1. **Modular Monolith over Microservices**: Built as a cohesive, single-deployable application. Avoids distributed transaction overhead, network latency, and operational complexity that is unwarranted for this domain.
2. **Polling over WebSockets for Chat**: Simple 5-second polling interval keeps the stack stateless and resilient without requiring WebSocket connection state management or Redis broker infrastructure.
3. **In-Memory Rate Limiting**: Single-instance IP sliding window rate-limiter prevents brute-force login attempts without requiring an external cache.
4. **Focused Tech Stack**: Explicitly rejected Kafka, Redis, Elasticsearch, Kubernetes, and AI buzzwords — prioritizing robust relational design, transaction boundaries, security hygiene, and complete test coverage that can be defended in technical interviews.
