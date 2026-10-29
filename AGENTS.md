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

Place backend tests under the matching package in `src/test/java`, using the `*Tests.java` naming pattern. Run them with `./mvnw test`. The frontend currently has lint and production-build checks rather than a configured test runner. Add focused tests when introducing behavior, especially around authentication, library rules, and API boundaries.

### Project-wide validation policy

Keep validation proportional to the change across the entire project, including backend, frontend, configuration, and documentation.

- For low-risk changes such as copy, icons, typography, spacing, minor animation adjustments, or documentation, review the relevant code and diff. Do not routinely run builds, lint, test suites, or browser checks.
- For behavior changes, run the smallest focused check that proves the affected behavior. Broaden validation only when the impact warrants it, a failure needs investigation, or the user explicitly requests it. Preserve necessary checks for authentication, authorization, database integrity, and other sensitive boundaries.
- Group related edits before validating. Run full builds, full lint, and broader test suites during preparation for a PR or deployment, according to the affected areas and risk, rather than after each edit.
- Reuse successful validation results when the relevant code and dependencies have not changed. Do not repeat checks merely because a commit, PR, or deployment is next; repeat them only when subsequent changes, failures, or unresolved concerns justify it.
- Browser checks should cover the affected interaction and viewport. Do not routinely check desktop, tablet, mobile, and reduced motion for every frontend change.

This project policy takes precedence over optional skill validation routines. Report briefly what was checked and any material limitation; do not expand validation beyond the task's needs.

## Commits and Pull Requests

Finish the requested changes and their proportional validation, then present the result to the user. Create commits and open pull requests only after the user's explicit confirmation of that completed result. A request to implement a feature, fix a bug, or make a hotfix does not authorize a commit or PR. Approval for a previous change does not authorize commits or PRs for subsequent work. Do not push, merge, or deploy unless the user has authorized those actions for the current completed change.

Recent history uses short, imperative Conventional Commit-style subjects, such as `feat: add library controls` and `fix: align catalog controls`. Use prefixes like `feat`, `fix`, or `hotfix(scope)`. Pull requests should explain the change and its reason, link related issues when applicable, and include screenshots for visible frontend changes. Before opening a PR, ensure the backend tests and frontend lint/build checks relevant to the change have passed, reusing valid earlier results under the validation policy above.

## Configuration and Secrets

Keep credentials out of commits. Configure local backend settings through environment variables (for example `TMDB_TOKEN`, `APP_JWT_SECRET`, and mail credentials); use `frontend/.env.local` for optional `VITE_API_URL`. Never use production secrets for local development.
