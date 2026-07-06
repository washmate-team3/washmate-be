# AutoWash Pro Auth/Security Test Report

## 1. Code reality check

### JWT

- Issuer code: `src/main/java/swp391/carwash/security/JwtService.java`.
- Signing: JJWT `signWith(key)` with `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`, so tokens are HMAC SHA (`HS*`) signed. Test secret length currently produces an `HS384` header.
- Access token TTL: `washmate.security.jwt.access-token-minutes` converted to seconds. Default in `application.properties` is `60` minutes.
- Refresh token TTL: `washmate.security.jwt.refresh-token-days` converted to seconds. Default is `7` days.
- Claims:
  - `sub`: user id as string.
  - `typ`: `access` or `refresh`.
  - `roles`: current active role names at token issuance.
  - `garageIds`: active role garage ids at token issuance.
  - `jti`, `iat`, `exp`.
- Authentication filter only trusts `sub` and `typ=access`; it reloads user, status, roles, and garage ids from DB via `AppUserDetailsService.loadUserById`.
- Refresh tokens are persisted only as SHA-256 URL-safe hashes in `refresh_token`; rotation revokes the old refresh token and stores the replacement hash.

### RBAC enforcement

- Global web security:
  - Public paths are listed in `PublicPaths`.
  - `GET /api/v1/garages` and `GET /api/v1/garages/**` are public.
  - `/api/admin/**` is restricted to `ADMIN` or `OWNER`.
  - all remaining paths require authentication.
- Method security is enabled with `@EnableMethodSecurity`; several controllers use `@PreAuthorize`.
- Service-level checks exist mainly for booking/payment/invoice ownership and garage operations.
- Gap found: `@PreAuthorize` denials currently become `500` through `GlobalExceptionHandler` instead of `403`.
- Gap found: service methods behind controller `@PreAuthorize` generally do not receive a principal, so direct service invocation can bypass controller-only RBAC.

### Google OAuth

- ID token verification is centralized in `GoogleAuthService.verifyIdToken`.
- It uses `GoogleIdTokenVerifier` with configured audience/client id, not payload-only decoding.
- It rejects missing Google client id, invalid ID token, and `email_verified=false`.
- Account linking is by normalized email: existing local/pending users are reused rather than duplicated.
- Existing pending users are activated on Google login.
- New Google users get `provider=GOOGLE`, empty password hash, active status, and default `CUSTOMER` role.
- `provider_id` is not present in `AppUser` or migrations. `avatar_url` exists but Google `picture` is not stored during OAuth login.

## 2. Endpoint and role table

| Endpoint | Method | Requirement in current code |
|---|---:|---|
| `/`, `/api/health`, `/actuator/health`, `/error`, `/favicon.ico` | GET | Public |
| `/api/auth/register`, `/api/auth/login`, `/api/auth/google`, `/api/auth/otp/request`, `/api/auth/otp/verify`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/password/forgot`, `/api/auth/password/reset` | POST | Public |
| `/api/auth/password/change` | PUT | Authenticated self |
| `/api/payments/vnpay/ipn`, `/api/payments/vnpay/return` | GET | Public |
| `/api/payments/{id}/vnpay/create-url` | POST | Authenticated; service requires owning `CUSTOMER` |
| `/api/admin/users`, `/api/admin/users/{userId}/status`, `/api/admin/invoices` | GET/PUT | `ADMIN` or `OWNER` via `SecurityFilterChain` |
| `/api/users/me`, `/api/users/me/avatar` | GET/PATCH/PUT/POST/DELETE | Authenticated self |
| `/api/bookings`, `/api/bookings/me`, `/api/bookings/{id}` and booking status actions | GET/POST/PUT | Authenticated; service enforces customer/owner/garage-operator rules |
| `/api/payments/{id}`, `/api/bookings/{bookingId}/payment`, `/api/payments/{id}/transactions` | GET | Authenticated; service allows booking owner or garage operator |
| `/api/payments/{id}/confirm`, `/fail`, `/cancel`, `/refund` | POST | Authenticated; service requires garage operator |
| `/api/invoices/{id}`, `/api/bookings/{bookingId}/invoice` | GET | Authenticated; service allows booking owner or garage operator |
| `/api/loyalty/me`, `/api/loyalty/transactions` | GET | Authenticated self |
| `/api/v1/notifications`, `/api/v1/notifications/{id}/read`, `/api/v1/notifications/read-all` | GET/PATCH | Authenticated self; service checks user id for single notification |
| `/api/v1/garages`, `/api/v1/garages/{id}` | GET | Public |
| `/api/v1/garages`, `/api/v1/garages/{id}` | POST/PUT/DELETE | Authenticated only; no role guard found |
| `/api/v1/garages/{garageId}/slots` | GET | Public because `GET /api/v1/garages/**` is permitted |
| `/api/v1/garages/{garageId}/slots`, `/api/v1/slots/{slotId}/capacity`, `/api/v1/slots/{slotId}` | POST/PUT/DELETE | `ADMIN`, `OWNER`, `STAFF` via `@PreAuthorize` |
| `/api/v1/vehicles` | GET | Out of scope for this auth/JWT review owner; current code uses `ADMIN` or `STAFF` via `@PreAuthorize` |
| `/api/v1/vehicles/my-vehicles` | GET/POST | Out of scope for this auth/JWT review owner; current code uses `CUSTOMER` via `@PreAuthorize` |
| `/api/v1/vehicles` | POST | Out of scope for this auth/JWT review owner; current code is authenticated only and accepts `userId` in request |
| `/api/v1/vehicles/{vehicleId}` | PUT/DELETE | Out of scope for this auth/JWT review owner; ownership should be handled by the Vehicle module owner |
| `/api/v1/services`, `/api/v1/services/{id}`, `/api/v1/services/garage/{garageId}` | GET/POST/PUT/DELETE | Authenticated only; no role guard found |
| `/api/v1/rewards`, `/api/v1/rewards/{rewardId}`, `/api/v1/rewards/all/{garageId}` | GET/POST/PUT/DELETE | Authenticated only; admin/staff `@PreAuthorize` is commented out for write endpoints |
| `/api/v1/rewards/{rewardId}/redeem` | POST | `CUSTOMER`, but method expects missing `garageId` path variable |
| `/api/v1/promotion/AvailablePromotions`, `/api/v1/promotion/manage/all` | GET | Authenticated only |
| `/api/v1/customer/loyalty`, `/api/v1/customer/loyalty/{id}` | GET/POST/PUT/DELETE | Authenticated only |
| `/api/v1/admin/loyalty-tiers`, `/api/v1/admin/loyalty-tiers/{id}` | GET/POST/PUT/DELETE | Authenticated only; does not match `/api/admin/**` |
| `/api/analytics/summary` | GET | `ADMIN` or `OWNER` via `@PreAuthorize` |
| `/api/v1/analytics/garage-owner/dashboard`, `/api/v1/analytics/admin/behavioral-logs`, `/api/v1/analytics/admin/customer-segments` | GET | Authenticated only; no role guard found |
| `/api/owner/insights/**`, `/api/owner/insight-rules/**`, `/api/owner/ai-*` | GET/POST/PATCH | `OWNER` or `ADMIN` via `@PreAuthorize` |
| `/api/test-vnpay-return` | GET | Authenticated only; appears to be a test/helper endpoint |

## 3. Generated tests

New files:

- `src/test/java/swp391/carwash/security/AuthenticationAuthorizationJwtSecurityTest.java`
- `src/test/java/swp391/carwash/security/JwtServiceClaimsSecurityTest.java`
- `src/test/java/swp391/carwash/service/TokenServiceSecurityLifecycleTest.java`
- `src/test/java/swp391/carwash/service/AuthServiceOAuthAndAbuseSecurityTest.java`
- `src/test/java/swp391/carwash/service/OwnershipAuthorizationServiceSecurityTest.java`

Command run:

```powershell
mvn -q '-Dtest=AuthenticationAuthorizationJwtSecurityTest,JwtServiceClaimsSecurityTest,TokenServiceSecurityLifecycleTest,AuthServiceOAuthAndAbuseSecurityTest,OwnershipAuthorizationServiceSecurityTest' test
```

Result: 46 tests discovered, 34 active passed, 12 skipped/disabled as documented current gaps, 0 failures, 0 errors.

## 4. Test status matrix

| Test case | Group | FR / business rule | Current status |
|---|---|---|---|
| Valid access token reaches authenticated endpoint | A | Authenticated user can call self endpoint | Pass |
| Expired access token rejected | A | Expired JWT must not authenticate | Pass |
| Tampered payload/signature rejected | A | Signature integrity required | Pass |
| Missing `roles` claim | A | Roles are loaded from DB, token role claim is not mandatory | Pass by current design |
| Missing `sub`/userId claim | A | Token without user id must fail-safe | Chua handle: disabled; current parser can leak non-ApiException |
| Wrong signing secret rejected | A | Only configured secret accepted | Pass |
| Missing/wrong Authorization header rejected | A | Protected endpoints require Bearer token | Pass |
| Refresh token expired/revoked/access-token-used-as-refresh rejected | A | Refresh lifecycle and rotation | Pass |
| Token role stale after DB role change | B | Next request should use latest DB role | Pass for DB reload; stale token role is ignored |
| User blocked/deleted after token issuance | B | Account status must be checked per request | Pass |
| Admin session revoke invalidates old access token | B | Immediate access-token invalidation | Chua handle: no access-token blacklist/session version |
| Role matrix via `/api/admin/**` | C | Wrong role returns 403, not 401 | Pass |
| Role matrix via `@PreAuthorize` endpoints | C | Wrong role should return 403 | Chua handle: currently returns 500 |
| Direct service call bypasses controller `@PreAuthorize` | C | RBAC must not depend only on controller | Chua handle: service methods lack principal/method security |
| No-role/unknown-role token fail-safe | C | User with no effective role should be denied | Chua handle for `@PreAuthorize` path because denial becomes 500 |
| Multi-role user gets all assigned permissions | C | Multi-role authorization union | Pass |
| Booking/payment/invoice IDOR read | D | User cannot read another user's resource | Pass |
| Booking update/cancel IDOR | D | User cannot mutate another user's booking | Pass |
| Vehicle update/delete IDOR | D | User cannot mutate another user's vehicle | Out of scope: Vehicle module is owned by another scope/team member |
| Google login links existing password account by email | E | Avoid duplicate user on same email | Pass |
| Fake/expired Google ID token rejected | E | Must verify with Google verifier | Pass |
| `email_verified=false` rejected | E | Unverified Google email must not login | Pass |
| Store Google `provider_id` and avatar | E | OAuth provider identity/profile linking | Chua handle: no `provider_id`; `avatar_url` not populated from Google |
| Repeated wrong password locks account | F | Brute-force mitigation | Pass |
| Login SQL injection payload does not bypass auth | F | Parameterized repository lookup | Pass |
| JWT `alg:none` rejected | F | Algorithm confusion defense | Pass |
