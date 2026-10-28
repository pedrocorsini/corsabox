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

### Frontend validation preference

Keep validation proportional to the change. Do not routinely run `npm run build`, `npm run lint`, or browser checks for desktop, mobile, and reduced motion after every small frontend edit (such as copy, icons, spacing, or a minor animation adjustment). Review the relevant code and diff instead. Run targeted checks when a change introduces meaningful behavior or risk, when investigating a failure, when explicitly requested, or before opening a PR as described below. Group related edits and avoid repeating checks unless new changes or failures justify them. This project preference takes precedence over optional frontend skill validation routines.

## Commits and Pull Requests

Recent history uses short, imperative Conventional Commit-style subjects, such as `feat: add library controls` and `fix: align catalog controls`. Use prefixes like `feat`, `fix`, or `hotfix(scope)`. Pull requests should explain the change and its reason, link related issues when applicable, and include screenshots for visible frontend changes. Before opening a PR, run backend tests and the frontend lint/build checks relevant to your changes.

## Configuration and Secrets

Keep credentials out of commits. Configure local backend settings through environment variables (for example `TMDB_TOKEN`, `APP_JWT_SECRET`, and mail credentials); use `frontend/.env.local` for optional `VITE_API_URL`. Never use production secrets for local development.
