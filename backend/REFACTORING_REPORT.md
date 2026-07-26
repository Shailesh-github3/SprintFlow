# Refactoring Report

## Latest Status

### Completed

- Phase 1 build restoration.
- Initial Phase 2 security hardening.
- Comment, message, issue, project, and invitation identity cleanup.
- Project and issue transaction hardening.
- Focused JWT unit test coverage.
- Project create/update DTO boundary cleanup.
- Project list/search principal cleanup.
- **Phase 5 RBAC and endpoint security completion:**
  - User role model with `ROLE_USER` default.
  - JWT token now carries role claims.
  - `CustomUserDetailsService` populates authorities from roles.
  - All `/api/**` controller methods secured with `@PreAuthorize("isAuthenticated()")`.
  - Missing authorization checks added to `IssueController`, `MessageController`, `ProjectController` read endpoints.
  - `InvitationService` hardened with expiration, one-time use, and duplicate prevention.
  - `GlobalExceptionHandler` now handles `SecurityException` with HTTP 403.
  - Four new security/authorization test classes added.

### Remaining

- Replace remaining entity-based response bodies with DTOs.
- Add pagination and sorting to list endpoints.
- Introduce mappers for entity/DTO conversion.
- Add OpenAPI documentation.
- Add broader integration tests.

## Notes

The codebase now has complete RBAC, method-level authorization, and endpoint security. All critical and high-priority items from the backend review are resolved. The application compiles and all security boundaries are enforced.