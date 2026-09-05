# SprintFlow Frontend

React 19 + Vite UI for the SprintFlow API (see root README for the full picture).

## Stack

React 19 · Vite 8 · React Router 7 · TanStack Query 5 · axios ·
react-hook-form + Zod · Radix/shadcn + Tailwind CSS 3 · Recharts · Sonner ·
lucide-react.

State: server state in React Query (per-feature keys + invalidation),
session in `AuthContext` (profile hydrated from `GET /api/users/me`),
theme in `ThemeContext`. No Redux.

## Setup

```bash
cp .env.example .env   # VITE_API_URL=http://localhost:8080
npm install
npm run dev            # http://localhost:5173 (backend on :8080, CORS preconfigured)
```

Auth uses the backend's HttpOnly `sprintflow_jwt` cookie (`withCredentials`),
so no token handling is needed client-side; only `{email, fullName, roles}`
is cached in `localStorage`.

## Scripts

- `npm run dev` — Vite dev server
- `npm run build` — production build (`dist/`, charts split into a lazy chunk)
- `npm run lint` — oxlint
- `npm run test` — Vitest + Testing Library (jsdom)
- `npm run preview` — serve the production build

## Routes

`/login`, `/register` (public) · `/` dashboard · `/projects`,
`/projects/:projectId` (issues + chat tabs, both paged) · `/invite/accept?token=`
(invitation landing) · `/subscription`, `/upgrade-plan/success` ·
`/settings` (all behind `ProtectedRoute`) · `*` → 404 page when authed,
login redirect otherwise.

## Notes

- List endpoints return Spring `Page` objects — queries unwrap
  `response.data.content ?? response.data`.
- Chat polls every 5s (no WebSockets by design).
- Destructive actions use an accessible `ConfirmDialog`, not `window.confirm`.
