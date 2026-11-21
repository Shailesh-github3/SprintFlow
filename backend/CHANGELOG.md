# Change Log / Remediation Plan

This file records the concrete changes recommended after the backend review.

## Phase 1 Completed: Build Stability Restored

The following build-blocking and startup-blocking items have been fixed in the codebase:

- Invalid `PlanType` enum syntax and price handling.
- Missing `@Entity` annotation on `Subscription`.
- Broken repository query methods for `Project`, `Comment`, `Message`, and `Subscription`.
- Invalid `UserRepository` methods tied to nonexistent JWT persistence.
- Missing `NoArgsConstructor` import in `IssueRequest`.
- `MessageController` class closure and exception handling.
- `PaymentController` constructor injection and removal of invalid JWT repository lookup.
- `UserService`, `IssueService`, `CommentService`, and `MessageService` null-safety and repository call cleanup.
- Basic JPA collection mapping for `Project.tags` and `Issue.tags`.
- Basic entity initialization for `Chat.messages`.

## Phase 2 Completed: Security Hardening

The following security improvements have been applied:

- Hardcoded datasource, mail, Razorpay, and JWT-related configuration was externalized to environment-driven properties.
- Added `JwtProperties` and `CorsProperties` for typed configuration.
- Introduced `JwtService` for injectable token generation and parsing.
- Converted the JWT token validator into a Spring-managed component.
- Updated the security filter chain to use the injected JWT filter.
- Added method-security support in the security configuration.
- Added validation dependency support and bean-validation annotations to request payloads.
- Added a global exception handler for validation and security failures.

## Phase 3 Completed: API and Service Cleanup

The following API and service improvements have been applied:

- Comments now derive the acting user from the authenticated principal instead of a client-supplied user ID.
- Message sending now derives the sender from the authenticated principal instead of `senderId` in the request body.
- Issue creation and deletion no longer accept unused client user IDs.
- Project creation, listing, and search now derive ownership/member context from the authenticated principal.
- Invitation acceptance now derives the joining user from the authenticated principal.
- Transaction boundaries were added to project and issue mutation methods.
- Added a focused JWT unit test to validate token generation and parsing.

## Phase 4 Completed: DTO Boundary Cleanup

The following API contract improvements have been applied:

- Project create/update endpoints now use a dedicated `ProjectRequest` DTO instead of the `Project` entity.
- Project create/update request payloads now use explicit validation annotations.
- Project list and search endpoints now always use the authenticated principal instead of a query-string user override.

## Phase 5 Completed: Role-Based Access Control and Endpoint Security

The following RBAC and endpoint security improvements have been applied:

- Added `roles` field to `User` entity with `@ElementCollection` for role storage.
- Updated `CustomUserDetailsService` to populate `GrantedAuthority` from user roles with `ROLE_` prefix.
- Updated `JwtService` to include roles claim in JWT token generation.
- Updated `JwtTokenValidator` to extract roles from JWT and populate authentication authorities.
- Updated `AuthController` to include default `ROLE_USER` authority in authentication token.
- Added `@PreAuthorize("isAuthenticated()")` to all `/api/**` controller methods.
- Added project membership/ownership authorization checks to `IssueController.getIssueById`, `getIssuesByProjectId`.
- Added project membership/ownership authorization checks to `MessageController.sendMessage`, `getMessagesByChatId`.
- Added project membership/ownership authorization checks to `ProjectController.getProjectById`, `getChatByProjectId`.
- Added `SecurityException` handler to `GlobalExceptionHandler` returning HTTP 403.
- Fixed `InvitationService` security issues: added token expiration (7 days), one-time use validation, duplicate prevention.
- Updated `Invitation` entity with `createdAt`, `expiryDate`, and `used` fields.
- Renamed `InvitationService.deletetoken` to `deleteToken` and `getTokenByUserMain` to `getTokenByUserEmail`.
- Added `AuthControllerSecurityTest` for registration endpoint security.
- Added `ProjectControllerAuthorizationTest` for project endpoint authorization.
- Added `IssueControllerAuthorizationTest` for issue endpoint authorization.
- Added `MessageControllerAuthorizationTest` for message endpoint authorization.
- Updated `AppConfig` to explicitly enable `prePostEnabled = true` for method security.

## Remaining Work (Medium/Low Priority)

- Replace remaining entity-based response bodies with DTOs (IssueDTO cleanup, Project responses).
- Add pagination and sorting to list endpoints.
- Introduce mappers for entity/DTO conversion.
- Add OpenAPI documentation.
- Add integration tests for full request flows.
- Clean up naming inconsistencies across the codebase.

## v1.0 Hardening Pass (Top-10 Fixes Applied)

- Fixed `PaymentController` generic-type compile error blocking the whole build.
- Closed comment-list IDOR: `GET /api/comments/issue/{id}` (and comment creation)
  now require project membership; added MockMvc regression test.
- Auth semantics: duplicate register -> 409 (`DuplicateResourceException`),
  bad login -> 401 (`InvalidCredentialsException`); generic 500 handler no
  longer echoes internal messages; `UserService.findByEmail` no longer echoes emails.
- Atomicity: new transactional `AuthService.register` (user + FREE subscription);
  new transactional `InvitationService.acceptInvitationForUser` (membership +
  consume in one transaction); invitation/invite flows return 400/403 typed errors.
- Domain enums: `IssueStatus` (TODO/IN_PROGRESS/DONE) + `IssuePriority`
  (LOW/MEDIUM/HIGH) enforced at DTO (`@Pattern`), service (`from()`), and DB
  (Flyway `V2__issue_constraints` CHECKs + `invitation.token`/`message.chat` indexes).
- Removed destructive `cascade=ALL + orphanRemoval` on `User.assignedIssues`;
  `GET /api/projects/search` is now paged.
- Tests: 57 green (new `UserControllerTest`, reworked auth/invitation/comment
  tests, `AuthServiceIntegrationTest` now exercises the real register path);
  `qa_tests/test_auth.js` rewritten to the real cookie/401/409 contract.
- Runnability: `backend/Dockerfile`, root `docker-compose.yml` (MySQL 8 +
  API, Flyway on boot), `.github/workflows/ci.yml` (backend `mvn verify` +
  frontend lint/build), `backend/.env.example` + `frontend/.env.example`.
- Frontend: fixed `Page.content` unwrapping crashes on project/issue lists
  (comments/chat rendered empty), mobile nav, reusable `ConfirmDialog`,
  honest dashboard stats, honest upgrade copy, Zod enums matching backend,
  `/invite/accept` page wired to the email link, `/api/users/me` profile
  hydration (fixes always-`N/A` role), removed unused deps/files.
- Docs: rewrote root + frontend READMEs to the actual implementation.

## Follow-up Hardening Pass

- Dead-code sweep: removed `IssueService.updateIssue` x2 (no endpoint),
  `ProjectService.removeTeamMember`, `ChatService` (+ its unused injection),
  `UserService.updateUserProjectSize`, `InvitationService.getTokenByUserEmail` /
  `deleteToken`, `AuthResponse`, and dead `JwtConstant.JWT_SECRET/JWT_EXPIRATION`
  (weak dev fallback no longer in source). Kept `CustomUserDetailsService` as
  the standard Spring Security DAO extension point.
- Deduped `MessageController` read endpoints (`/project/{id}` removed,
  `/chat/{projectId}` kept — the one the UI calls); fixed the authz test path.
- Unified error shape: `ApiErrorResponse` gained an optional `fields` map
  (omitted when null), validation errors now carry a `message` too, and
  `ProjectController` try/catch blocks were removed in favor of the global
  handler (also fixes update/delete returning 404 on authorization failures).
- Rate limiting: `RateLimitFilter` (20 req/min/IP sliding window on
  `/api/auth/**`, 429 + standard error shape, in-memory single-instance,
  X-Forwarded-For aware) wired before the JWT filter, with 4 unit tests.
- Invitation emails are now `@Async` (`@EnableAsync`); failures stay logged,
  invite requests no longer block on SMTP.
- Frontend: server-side project search (debounced) + pager on project/issue
  lists (`useDebounce`, `Pagination`), 404 page for authed wildcard routes,
  "session expired" notice on 401 redirect, Recharts split into a lazy
  `charts` chunk (vendor 908KB -> 545KB), Vitest + Testing Library setup
  with 5 passing tests (form validation, confirm dialog, login flow).
- Payments explicitly gated to test-mode links only in the README (no real
  money; webhook tracked as future work).

## Minimal Audit Log

- New append-only `audit_log` table (Flyway `V3__audit_log`): actor, project,
  entity type (`ISSUE` / `PROJECT_MEMBER`), entity id, action, nullable
  before/after snapshots, timestamp; indexed by project + time.
- `AuditService.log` runs inside the caller's transaction (failed operations
  leave no phantom lines) and renders human-readable `summary` strings
  ("Priya changed issue #42: TODO → IN_PROGRESS").
- Recorded events: issue created / assigned (old → new assignee, with
  "Unassigned" default) / status changed (old → new), member invited
  (by whom, to whom), member joined.
- Read endpoint `GET /api/projects/{id}/activity` (paged, member-only).
- Tests: summary cases, recording wiring for all five events, endpoint
  200/403; invite/accept tests extended with audit verifications.

## Phase 1: Security & Stability Hardening (Completed)

- **Subscription Upgrade Security**:
  - Removed unauthenticated backdoor `PATCH /api/subscriptions/update/{planType}` from `SubscriptionController`.
  - Added `PaymentVerificationRequest` DTO and `POST /api/payments/verify` in `PaymentController` requiring authenticated user context and HMAC-SHA256 signature verification via `PaymentService.completePayment()`.
  - Injected `frontendUrl` into `PaymentService` to fix hardcoded `https://localhost:5173` callback URLs in Razorpay payment link generation.
  - Standardized `SubscriptionController` responses to return `SubscriptionDTO` rather than raw error strings.
- **Invitation Multi-Tenancy Fix**:
  - Replaced single-result `findByUserEmail` query in `InvitationRepository` with `findByUserEmailAndProjectId(email, projectId)` to eliminate `NonUniqueResultException` (`IncorrectResultSizeDataAccessException`) when a user is invited to multiple projects.
- **Frontend Auth & UX Interceptor Fix**:
  - Fixed Axios 401 response interceptor in `api.js` to exclude `/api/auth/login` and `/api/auth/register`, ensuring authentication errors display inline form feedback instead of causing hard reloads and flashing irrelevant "session expired" notices.
  - Updated `UpgradeSuccess.jsx` to submit transaction parameters (`razorpayOrderId`, `razorpayPaymentId`, `razorpaySignature`) to `POST /api/payments/verify`.
- **Secret & File Hygiene**:
  - Cleaned up untracked debug files (`cookiejar.txt`, `cookiejar2.txt`) from disk.

## Phase 2: Database & Query Performance Remediation (Completed)

- **Eliminated Hibernate In-Memory Pagination Hazard**:
  - In `ProjectRepository.findProjectsForMemberWithFilters`, replaced collection fetch `attributePaths = {"issues", "projectOwner"}` with to-one associations `attributePaths = {"projectOwner", "chat"}`. This eliminates the `HHH000104` warning where Hibernate fetched all issues into JVM memory to perform in-memory pagination.
  - In `IssueRepository.findByProject_ProjectId`, removed collection fetch `"comments"` from `@EntityGraph`, keeping only `"assignedUser"`.
- **Eliminated N+1 Query Loops via Hibernate Batch Fetching**:
  - Enabled `spring.jpa.properties.hibernate.default_batch_fetch_size=20` in `application.properties`.
  - Added `@BatchSize(size = 20)` to `Project.tags`, `Project.projectMembers`, and `Issue.tags`. When mapping paginated DTO results, collections are batch-fetched across the page in single `IN (?, ?...)` queries rather than N individual queries.
- **Fixed JPA Entity Index Column Mappings**:
  - Corrected `Issue.java` `@Table` index column name from `project_id` to actual DB column `project_project_id`.
  - Corrected `Comment.java` `@Table` index column name from `issue_id` to actual DB column `issue_issue_id`.
- **Project Filter Query Alignment**:
  - Aligned `ProjectRepository.findProjectsForMemberWithFilters` WHERE clause to `(m = :user OR p.projectOwner = :user)` so owners consistently retrieve their projects regardless of explicit member table presence.
- **Tests**:
  - Updated `RepositoryIntegrationTest` to assert initialization of `projectOwner` and `chat`. All 67 backend test cases pass cleanly.

