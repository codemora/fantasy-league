# 0008: JWT authentication with revocable refresh tokens

## Status
Accepted

## Context
Issue #35 (register/log in) left the auth mechanism unspecified ("a session/token used to authenticate subsequent requests"). The README describes this project as "an API for a fantasy league," implying clients are decoupled from the server — a separate frontend and/or mobile app — rather than a server-rendered app where cookie-based `HttpSession` would be the natural default. Two realistic choices:
- **Session-based**: server holds session state (in-memory or a session store), client holds an opaque session cookie. Simple, trivially revocable, but couples every request to server-side session storage and is awkward for a mobile client or a frontend on a different origin.
- **JWT (stateless bearer tokens)**: server signs a token containing the user's identity and role; client sends it as `Authorization: Bearer <token>`. No server-side lookup needed to validate a request, which fits a pure API consumed by arbitrary clients — but a stateless token can't be revoked before it expires, which is a real problem if a user needs to be logged out (e.g. compromised credentials) before their token naturally expires.

## Decision
JWT access tokens, short-lived (15 minutes), signed HS256, containing `user_id` and `role`. Paired with an opaque, randomly-generated **refresh token**, stored server-side (hashed) in a `RefreshToken` table (`id`, `user_id`, `token_hash`, `expires_at`, `revoked_at`), long-lived (30 days), rotated on every use (each refresh invalidates the old refresh token and issues a new one).

This hybrid gets both properties: request authentication is stateless (validate the JWT signature and expiry, no DB lookup), while the refresh token gives a real point of revocation — logging a user out, or an admin forcibly ending a session, means deleting/marking-revoked that one `RefreshToken` row.

Endpoints: `POST /auth/register`, `POST /auth/login` (returns access + refresh token pair), `POST /auth/refresh` (exchanges a valid refresh token for a new pair), `POST /auth/logout` (revokes the refresh token). Spring Security's filter chain validates the JWT on every request; `role` (`ADMIN` / `USER`, per the `User.role` field in the README model) drives endpoint authorization.

## Consequences
- Access tokens can't be revoked before they expire — a compromised access token is valid for up to 15 minutes even after logout. This is the standard, accepted tradeoff of the stateless-access/revocable-refresh pattern, and 15 minutes bounds the exposure window.
- Every protected endpoint needs the JWT validation filter; no endpoint can silently rely on `HttpSession` state.
- Issue #35's acceptance criteria should be read as "session/token" now meaning specifically this JWT + refresh-token pair, not a `HttpSession` cookie.
- Requires a `jjwt` (or equivalent) dependency and a signing secret managed outside version control (e.g. an environment variable) — this repo's `.env` file (see `.gitignore`) is the natural place for local development.
