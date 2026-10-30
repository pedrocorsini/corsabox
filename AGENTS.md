# Repository Guidelines

## Project Structure

The backend is a Java 25/Spring Boot application under `src/main/java/com/corsini/corsabox`. It is organized by feature (`identity`, `catalog`, `library`), with API, application, domain, and infrastructure layers where appropriate. Backend configuration is in `src/main/resources`; tests live in `src/test/java`. The React 19 and TypeScript frontend is in `frontend/src`, split into `pages`, `components`, `auth`, and `lib`. Static assets are in `frontend/public`. `compose.yaml` provides local PostgreSQL.

## Build, Test, and Development

- `docker compose up -d` starts the local database.
- `./mvnw spring-boot:run` starts the API (Windows: `mvnw.cmd spring-boot:run`).
- `./mvnw test` runs backend tests.
- `cd frontend && npm install && npm run dev` installs dependencies and starts the Vite development server.
- `cd frontend && npm run build` type-checks and builds the frontend; `npm run lint` runs ESLint.

## Coding Style

Follow nearby code conventions. Use standard Java formatting, package names in lowercase, and descriptive `UpperCamelCase` class names. Keep backend responsibilities in their existing feature and layer. Frontend components and page files use `UpperCamelCase` (for example, `CatalogPage.tsx`); hooks and helpers use `lowerCamelCase`. Use TypeScript types for component props and API data, and keep shared UI, auth, and API helpers in their existing directories. ESLint is configured in `frontend/eslint.config.js`; no separate formatter is configured.

## Testing

When tests are requested, place backend tests under the matching package in `src/test/java`, using the `*Tests.java` naming pattern. They can be run with `./mvnw test`. The frontend currently has lint and production-build checks rather than a configured test runner. Adding or running tests is not a requirement for completing a change.

### Project-wide validation policy

Default to reviewing the relevant code and diff across the entire project, including backend, frontend, configuration, and documentation. Automated validation is optional and must not delay delivery.

- Do not run tests, builds, type-checking, lint, browser checks, or other validation scripts unless the user explicitly requests those checks for the current task. Implementing a feature, fixing a bug, or requesting a commit, PR, or deployment does not implicitly request validation runs.
- Do not ask for permission to run optional checks as a routine final step. Complete the implementation and present the result after code and diff review.
- Preserve runtime input validation, authentication, authorization, database constraints, and other application safeguards. This policy removes mandatory verification runs, not safeguards in the implementation.
- When the user explicitly requests validation, run only the smallest checks covering that request, group related edits first, and reuse valid earlier results. Do not broaden or repeat checks without a concrete need within the requested scope.

This project policy takes precedence over optional skill validation routines. Briefly state that checks were not run when relevant; never claim unexecuted checks passed.

## Commits and Pull Requests

Finish the requested changes and review the code and diff, then present the result to the user. Run validation only when explicitly requested under the policy above. Create commits and open pull requests only after the user's explicit confirmation of that completed result. A request to implement a feature, fix a bug, or make a hotfix does not authorize a commit or PR. Approval for a previous change does not authorize commits or PRs for subsequent work. Do not push, merge, or deploy unless the user has authorized those actions for the current completed change.

Recent history uses short, imperative Conventional Commit-style subjects, such as `feat: add library controls` and `fix: align catalog controls`. Use prefixes like `feat`, `fix`, or `hotfix(scope)`. Pull requests should explain the change and its reason and link related issues when applicable. Include existing screenshots for visible frontend changes when available; capturing them is not a prerequisite. Tests, lint, builds, and browser checks are not prerequisites for a commit, PR, or deployment. Report available validation results accurately, including when checks were not run.

## Configuration and Secrets

Keep credentials out of commits. Configure local backend settings through environment variables (for example `TMDB_TOKEN`, `APP_JWT_SECRET`, and mail credentials); use `frontend/.env.local` for optional `VITE_API_URL`. Never use production secrets for local development.
