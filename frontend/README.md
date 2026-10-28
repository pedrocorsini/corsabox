# CorsaBox frontend

React 19 and TypeScript application built with Vite and Tailwind CSS 4.

## Development

Use Node.js 22.22 or newer. Start the backend at `http://localhost:8080` following the [project setup instructions](../README.md#running-locally), then run from this directory:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to the backend. To use a different API origin, set `VITE_API_URL` in `.env.local` and restart Vite. This variable is embedded at build time; do not put secrets in it.

## Checks and production build

```bash
npm run lint
npm run build
```

The build type-checks the application and writes production assets to `dist/`. Run `npm run preview` to inspect the build locally. The development API proxy is not available in preview or production: configure `VITE_API_URL` before building or serve `/api` through a reverse proxy. Hosting must also route client-side page URLs to `index.html`.

## Source layout

- `src/pages`: application pages.
- `src/components`: shared UI and feature components.
- `src/auth`: authentication state and route guards.
- `src/lib`: API client, types, helpers and hooks.
- `public`: static assets and SEO files.

See the [root README](../README.md) for features, backend configuration and the MIT license.
