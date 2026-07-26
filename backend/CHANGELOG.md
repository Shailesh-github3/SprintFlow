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