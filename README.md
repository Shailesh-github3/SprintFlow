# SprintFlow — Project & Issue Tracker

A backend-focused full-stack project-management app: users register, create
projects, invite team members by email, track issues (status/priority/assignee),
discuss via comments and per-project chat, and manage FREE/MONTHLY/YEARLY
subscriptions with Razorpay payment links.

**Stack (what's actually used):** Java 21 · Spring Boot 3.5 (Web, Security,
Data JPA, Validation, Mail) · MySQL 8 + Flyway · JWT in HttpOnly cookies ·
MapStruct · springdoc-openapi · React 19 + Vite · React Router 7 ·
TanStack Query 5 · react-hook-form + Zod · Radix/shadcn + Tailwind · axios ·
Recharts · Sonner. No Redux, no microservices, no AI — deliberately.

## Architecture

```
Browser (React/Vite) ──HTTPS/JSON + HttpOnly cookie──> Spring Boot API ──JPA──> MySQL 8
                        Swagger UI at /swagger-ui.html   Flyway migrations ──> (V1 schema, V2 constraints)
                                                         SMTP (Gmail) ──> invitation emails
                                                         Razorpay ──> payment links
```

Layered monolith: `controller → service → repository → entity`, MapStruct
DTOs at the API boundary, imperative ownership/membership checks
(`ProjectService.ensureProjectOwner / ensureProjectMemberOrOwner`), global
`ApiErrorResponse` error shape.

## Features (honest status)

| Feature | Status |
|---|---|
| Register / login / logout (BCrypt + HttpOnly JWT cookie) | Working |
| Project CRUD, list (paged, category/tag filter), search (paged) | Working |
| Email invitations (7-day, one-time, atomic accept) + in-app accept page | Working |
| Issue CRUD, assign (owner-only), status transitions | Working |
| Issue comments (member-only read/write, author-only delete) | Working |
| Per-project chat (5s polling — not WebSockets) | Working |
| Project activity feed (append-only audit log with summaries) | Working |
| Subscriptions FREE/MONTHLY/YEARLY, Razorpay payment-link creation | Working (test-mode links only) |
| Plan upgrade via payment redirect (no webhook verification yet) | Partial — test mode only, see Limitations |
| Dashboard stats, settings/profile, upgrade flow | Working |

## Quick start

Prereqs: Docker (for MySQL + API), Node 22, Java 21 + Maven (only for
backend dev outside Docker).

```bash
# 1. Configure (never commit .env files)
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
# edit backend/.env: set SPRINGFLOW_JWT_SECRET (openssl rand -hex 32),
# DB/mail/Razorpay values as needed.

# 2. Start MySQL + API (Flyway migrates automatically)
docker compose up --build

# 3. Start the UI
cd frontend && npm install && npm run dev
```

Open the app at http://localhost:5173, API at http://localhost:8080,
Swagger UI at http://localhost:8080/swagger-ui.html.

## Testing

```bash
cd backend && mvn verify        # unit + MockMvc authz matrix (needs MySQL, see CI)
cd frontend && npm run lint && npm run build && npm test
```

- Backend: 70 tests green — controller auth tests (incl. 401/409/429,
  comment-list IDOR regression, activity-feed authz), service unit tests,
  audit summary + recording tests, rate-limiter tests, MapStruct mapper
  tests, Testcontainers repository integration tests
  (skip gracefully without Docker).
- Frontend: 5 Vitest + Testing Library tests (form validation, confirm
  dialog, login flow) run in jsdom.
- `backend/qa_tests/*.js` are legacy manual scripts for a live server;
  MockMvc tests are the source of truth.
- CI (`.github/workflows/ci.yml`) runs backend `mvn -B verify` against a
  MySQL 8.0 service plus frontend lint + build on every push/PR.

## Database

- `backend/src/main/resources/db/migration/V1__initial_schema.sql` — full
  schema (users, roles, projects, members, issues, comments, chats, messages,
  invitations, subscriptions), InnoDB/`utf8mb4`, FK constraints, unique email.
- `V2__issue_constraints.sql` — CHECK constraints locking issue
  status ∈ {TODO, IN_PROGRESS, DONE} and priority ∈ {LOW, MEDIUM, HIGH},
  plus indexes on `invitation.token` and `message.chat`.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate never mutates the
  schema; Flyway owns it.

## Security notes

- Passwords: BCrypt. Sessions: stateless JWT in HttpOnly `SameSite=Lax`
  cookie (`Authorization: Bearer` also accepted). CSRF disabled as for a
  same-origin cookie demo — locked origins via `SPRINGFLOW_CORS_ALLOWED_ORIGINS`.
- Every `/api/**` route requires authentication; project data additionally
  requires owner/member checks (a previous comment-list IDOR is covered by a
  regression test).
- Auth failures return generic 401/409 messages (no user enumeration).
- Secrets live only in `.env` (gitignored, see `.env.example`). A debug JWT
  once committed in `cookiejar2.txt` was removed; rotate
  `SPRINGFLOW_JWT_SECRET` if that token ever touched a shared environment.

## Known limitations & trade-offs

1. Payments are **test-mode links only**: the app creates Razorpay payment
   links with test keys and updates the plan on redirect back. There is no
   server-side webhook/signature verification (`PaymentService.completePayment`
   is the reserved hook), so **do not accept real money** with this build.
   Wiring the webhook is tracked future work.
2. Auth endpoints are rate-limited in-memory (20 req/min/IP, single instance).
3. Chat is 5s polling, not WebSockets — fine for a demo, stated honestly.
4. No assigned-issues-across-projects endpoint yet (dashboard shows only
   project-derived stats).
5. Single-layer roles (`USER` default); project owner/member checks are
   enforced imperatively in services, not via role annotations.

## Future work (explicitly *not* added)

Redis, Kafka, Elasticsearch, WebSockets, microservices, Kubernetes, AI
summarization — none solves a problem this codebase has; each would add
complexity a fresher couldn't defend. See the audit for the full
build-vs-buy reasoning.
