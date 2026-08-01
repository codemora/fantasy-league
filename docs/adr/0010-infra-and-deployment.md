# 0010: CI, containerization, secrets, and deployment target

## Status
Accepted

## Context
No CI pipeline, container image, deployment target, or production secrets management existed — `.github/` only contains unrelated tooling scaffolding, and the only `.env` in the repo is for local scripting, not application config. ADR 0003 (Postgres + Flyway, H2 for tests) and ADR 0008 (JWT signing secret) both assume *something* handles schema application and secret injection outside the test profile, but neither says what. This project is a small, likely solo/small-team-maintained app, not one with an existing scaling or compliance requirement — so the goal here is the lowest-ops-burden setup that's still correct, not maximal control.

## Decision

**CI: GitHub Actions.** On push/PR to `main`: run `./mvnw verify` on Java 21 with Maven dependency caching. In addition to the H2-backed test profile (ADR 0003), CI also spins up a Postgres service container and applies the Flyway migrations against it directly — a migration can be valid H2 SQL and still break on real Postgres syntax/behavior, and this catches that before merge rather than in production. Add a `dependabot.yml` for dependency vulnerability alerts (no extra tooling — GitHub-native), and a branch-protection rule requiring the CI check to pass before merging to `main`.

**Containerization: Spring Boot Buildpacks**, not a hand-written Dockerfile. `./mvnw spring-boot:build-image` produces an optimized, layered OCI image using Cloud Native Buildpacks, built into the `spring-boot-maven-plugin` already in `pom.xml`. For local development, a `docker-compose.yml` runs the app alongside a Postgres container, so contributors never need Postgres installed natively.

**Secrets: environment variables at runtime, never baked into the image** (12-factor style) — this is how Spring Boot already expects config overrides (`SPRING_DATASOURCE_PASSWORD`, a `JWT_SIGNING_SECRET` for ADR 0008's signing key, etc.):
- **Local**: `.env` (already gitignored).
- **CI**: GitHub Actions encrypted repo/environment secrets.
- **Production**: Render's environment variable UI (see below).

**Deployment target: Render.** A web service deployed straight from `main` on GitHub (git-push-to-deploy), plus Render's managed Postgres add-on for the database. Chosen over a self-managed VPS (Hetzner/DigitalOcean + Docker Compose) and over AWS (ECS Fargate/App Runner + RDS): a VPS trades convenience for owning TLS, OS patching, and backups yourself; AWS trades convenience for control (VPC, IAM, scaling policies) that this project has no current need for. Render and Railway were both reasonable PaaS choices — Render was picked for its more predictable pricing model and explicit free-tier Postgres policy, which suits a side project better than usage-based pricing.

## Consequences
- Deploying is `git push` to `main` — no infrastructure-as-code, no manual server management, TLS handled automatically.
- Real vendor coupling: Render-specific environment/build configuration would need reworking to move to a different PaaS or to AWS/a VPS later. Accepted tradeoff for the ops-time saved now; revisit if the project outgrows Render's free/low tiers or needs infra control Render doesn't offer (e.g. VPC peering, custom compliance requirements).
- CI running migrations against real Postgres (not just H2) adds a small amount of CI time in exchange for catching a category of bug (Postgres-specific migration issues) that ADR 0003's H2 test profile can't.
- Using Buildpacks instead of a Dockerfile means less control over the base image and OS layer, but zero Dockerfile to write or maintain — acceptable since there's no current need for a customized runtime image.
