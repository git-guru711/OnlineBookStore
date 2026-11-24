# Bookstore frontend

The React single-page app of the Online Bookstore: browse and search books, log in or register, fill a cart, check out and see your orders. It talks to the Spring Boot backend in [`../backend`](../backend) through its REST API (`/api/v1`). To run the whole project, see the [main README](../README.md).

**Stack:** React 19 · TypeScript (strict) · Vite 7 · Material UI 9 · TanStack Query 5 · React Router 7 · Vitest 5 + React Testing Library + MSW · ESLint 10

---

## Contents

1. [Commands](#1-commands)
2. [Dependencies and versions](#2-dependencies-and-versions)
3. [Folder structure](#3-folder-structure)
4. [How it flows](#4-how-it-flows)
5. [Best practices followed](#5-best-practices-followed)

---

## 1. Commands

Run inside `frontend/`, after `npm install` (Node.js 20 or later). Start the backend first.

| Command | What it does |
|---|---|
| `npm run dev` | Dev server on http://localhost:5173 with hot reload; forwards `/api` to the backend |
| `npm test` | Vitest in watch mode: re-runs tests on every save (for TDD) |
| `npm run test:run` | All 190 tests once |
| `npm run coverage` | All tests with coverage; fails below 80% of lines, statements, functions or branches (report: `coverage/index.html`) |
| `npm run lint` | ESLint: likely bugs, React rules and the folder architecture |
| `npm run format` | Prettier: formats all files |
| `npm run format:check` | Fails if any file is not formatted (for CI and before committing) |
| `npm run e2e` | Playwright end-to-end tests in a real browser against the real backend (starts the backend and dev server if they are not running; first time: `npx playwright install chromium`) |
| `npm run typecheck` | TypeScript check only |
| `VITE_API_BASE_URL=/api/v1 npm run build` | Type check, then the production build into `dist/` |
| `npm run preview` | Serves the built `dist/` on http://localhost:5173 |

Log in with the demo account `demo@example.com` / `demo1234`, or register.

### Configuration

The backend's address is declared in `.env.development`, never hard-coded:

```
VITE_API_BASE_URL=/api/v1              where the app sends API calls (same origin as the page)
BACKEND_URL=http://localhost:8080      where the dev server forwards /api (never reaches the browser)
```

| File | Used by | In git? |
|---|---|---|
| `.env.development` | `npm run dev` | yes (nothing secret) |
| `.env.test` | tests (`http://localhost/api/v1`, answered by the MSW fake backend) | yes |
| `.env.development.local` | your machine only, e.g. `BACKEND_URL=http://localhost:9090` | no |

Only variables starting with `VITE_` reach the browser, so nothing secret may go in one. A production build gets the address at build time (there is no production `.env` file).

---

## 2. Dependencies and versions

Versions are the ones installed from `package-lock.json`, which pins every package so all machines get the same install.

### Runtime (`dependencies`)

| Package | Version | Used for |
|---|---|---|
| `react`, `react-dom` | 19.3.1 | UI components and rendering |
| `react-router-dom` | 7.18.6 | Pages and URLs without page reloads (`/home`, `/cart`, `/orders/:id`) |
| `@tanstack/react-query` | 5.108.2 | Server data: fetching, caching, loading/error states, refresh after changes |
| `@mui/material` | 9.5.0 | Ready-made accessible components (buttons, cards, tables, dialogs) and theming |
| `@mui/icons-material` | 9.5.0 | Icons (cart, account, add/remove) |
| `@emotion/react`, `@emotion/styled` | 11.14.0 / 11.14.1 | The styling engine Material UI uses |
| `@fontsource-variable/playfair-display` | 5.3.0 | Heading font, bundled with the app (no external font server) |
| `@fontsource-variable/source-sans-3` | 5.3.1 | Body font, bundled with the app |

### Development (`devDependencies`)

| Package | Version | Used for |
|---|---|---|
| `typescript` | 5.9.3 | Static types (strict mode) |
| `vite` | 7.3.8 | Dev server with hot reload, `/api` proxy, production build |
| `@vitejs/plugin-react` | 5.2.0 | JSX and React Fast Refresh for Vite |
| `vitest` | 5.1.1 | Test runner (same config and `@/` alias as Vite) |
| `jsdom` | 27.4.0 | A browser-like DOM for tests in Node |
| `@testing-library/react` | 16.3.3 | Render components and find elements the way users do (by role and name) |
| `@testing-library/user-event` | 14.6.13 | Realistic clicks and typing in tests |
| `@testing-library/jest-dom` | 6.10.0 | Readable assertions: `toBeInTheDocument()`, `toBeDisabled()` |
| `msw` | 2.15.0 | Mock Service Worker: a fake backend for tests, answering real HTTP requests |
| `@vitest/coverage-v8` | 5.1.1 | Test coverage (V8's built-in coverage) for `npm run coverage` |
| `@playwright/test` | 1.65.0 | End-to-end tests: drives a real Chromium browser through the running app |
| `eslint` | 10.15.0 | Linting |
| `@eslint/js`, `typescript-eslint` | 10.0.1 / 8.73.0 | JavaScript and TypeScript rules |
| `eslint-plugin-react-hooks` | 7.1.1 | Rules of hooks, dependency arrays |
| `eslint-plugin-react-refresh` | 0.5.8 | Keeps files compatible with hot reload |
| `prettier` | 3.9.10 | Code formatter (single quotes, 120 columns, trailing commas) |
| `eslint-config-prettier` | 10.1.8 | Turns off ESLint rules that would conflict with Prettier |
| `globals` | 17.14.0 | Browser globals for ESLint |
| `@types/react`, `@types/react-dom` | 19.3.0 | TypeScript types for React |

---

## 3. Folder structure

Feature-based: code is grouped by **what it is about** (books, cart, ...), and truly shared code lives in a few shared folders.

```
frontend/
├── index.html              the single HTML page; React starts in <div id="root">
├── package.json            dependencies and npm scripts
├── package-lock.json       exact versions of every installed package
├── vite.config.ts          dev server (port 5173, /api proxy), "@/" alias, test settings
├── tsconfig.json           TypeScript settings (strict, "@/" alias)
├── eslint.config.js        lint rules, including the folder architecture
├── .prettierrc.json        formatting rules (Prettier); .prettierignore lists what is not formatted
├── playwright.config.ts    end-to-end tests: browser, base URL, starts backend and dev server
├── tests/                  end-to-end tests (Playwright), outside src/: journey.spec.ts, stock.spec.ts
├── .env.development        backend address for development
├── .env.test               API address for tests
└── src/
    ├── main.tsx            entry point: loads the fonts and renders <App />
    ├── app/                the application shell
    │   ├── App.tsx         providers + browser router
    │   ├── provider.tsx    AppProviders: data cache, auth, theme
    │   ├── router.tsx      the route table; combines features (e.g. books + "Add to cart")
    │   ├── theme.ts        colours, fonts, shapes: the whole look in one file
    │   ├── layout/         Layout (header + page + footer), AccountMenu, Footer
    │   ├── pages/          NotFoundPage
    │   └── __tests__/      flow tests that cross features
    ├── features/           one folder per domain area, all with the same inner layout
    │   ├── auth/           login, registration, session, protected routes
    │   ├── books/          catalog: cards with covers, search, pagination, banner
    │   ├── cart/           add to cart, header badge, cart page, checkout
    │   └── orders/         order confirmation and order history
    ├── components/         UI used by several features: ErrorMessage, EmptyState, CheckoutSteps
    ├── lib/                HTTP client, error type, query client, env settings, paths (HOME_PATH)
    ├── types/              shared API shapes: PageResponse<T>, StockStatus
    ├── utils/              small pure helpers: formatPrice, formatDate, pageFromAddress
    └── testing/            test-only code: setup, render helpers, test data, MSW fake backend
```

### Inside every feature

```
features/<name>/
├── api/          HTTP calls only                      e.g. cart/api/cart.ts
├── hooks/        TanStack Query hooks                 e.g. cart/hooks/useCart.ts
├── components/   UI pieces of this feature            e.g. cart/components/QuantityInput.tsx
├── pages/        screens that a route shows           e.g. cart/pages/CartPage.tsx
├── context/      React context (auth only)            auth/context/AuthProvider.tsx
├── utils/        pure helpers                         e.g. cart/utils/cartErrorMessage.ts
├── __tests__/    flow tests: whole user journeys      e.g. cart/__tests__/cart.flow.test.tsx
├── types.ts      TypeScript shapes of the backend's JSON
└── index.ts      the feature's public API: the only file others may import
```

A folder exists only once it has a file, so not every feature has every folder.

### Where the tests are

- **Unit and component tests sit next to the file they test**: `QuantityInput.tsx` + `QuantityInput.test.tsx`.
- **Flow tests** (multi-step user journeys) are in each feature's `__tests__/`, or in `app/__tests__/` when they cross features.
- **Shared test helpers** are in `src/testing/`; production code may not import them (ESLint rule).
- **End-to-end tests** are in `tests/` (`*.spec.ts`), outside `src/`: Playwright clicks through the real app in Chromium against the real backend. Vitest never runs them, and they never reach the production bundle.

### Import rule

```
app/  ──►  features/  ──►  components/ · lib/ · types/ · utils/
```

Dependencies flow one way. Features never import each other: the router combines them (the books page shows the cart's "Add to cart" button through a `renderBookAction` prop). Other features use auth only through `@/features/auth`. ESLint turns a forbidden import into an error.

---

## 4. How it flows

### Routes

| URL | Page | Login |
|---|---|---|
| `/` | redirects to `/home`, keeping the query (`/?page=2` → `/home?page=2`) | – |
| `/home`, `/home?page=2`, `/home?q=martin` | `BooksPage`: catalog, search, pages | no |
| `/login`, `/register` | `LoginPage`, `RegisterPage` (back to `?redirect=` afterwards) | no |
| `/cart` | `CartPage` | yes |
| `/checkout` | `CheckoutPage` | yes |
| `/orders`, `/orders/:id` | `OrdersPage`, `OrderPage` | yes |
| anything else | `NotFoundPage` | – |

Protected pages are wrapped in `ProtectedRoute`: a guest is sent to `/login?redirect=/cart` and returns there after logging in.

### From a click to the backend and back

Example: the catalog page loads, then the user adds a book to the cart.

```
BooksPage (features/books/pages)
   │  reads ?page and ?q from the URL
   ▼
useBooks({ page, q })   (features/books/hooks)     TanStack Query: one cache entry per page and search,
   │                                                loading / error / data states
   ▼
getBooks()              (features/books/api)       builds /books?page=0&size=12&q=...
   ▼
apiClient               (lib/api-client.ts)         base URL from lib/env.ts, Basic auth header if logged in,
   │                                                JSON in/out, every error → ApiError (status, type, detail)
   ▼
Vite dev server :5173 ──/api proxy──► Spring Boot :8080
   │
   ▼
cards rendered; each card gets <AddToCartButton> from the router (cart feature)
   │  click
   ▼
useAddToCart().mutate   (features/cart/hooks)       POST /cart/items; the server's CartResponse is written
                                                    into the cache, so the header badge and the cart page
                                                    update at once without another request
```

### Login

```
LoginPage ─► useAuth().login(email, password) ─► GET /auth/user with Basic auth
                 │ 200: user + credentials saved in sessionStorage (per tab), header shows the name
                 │ 401: "Invalid email or password" (never says which one was wrong)
                 ▼
            navigate to ?redirect= (only paths inside this app) or /home
```

A later 401 from any call (e.g. the password changed) logs the user out and clears the cache.

---

## 5. Best practices followed

**Testing**
- **Test-driven development**: every feature started with a failing test, then the code to make it pass. Tests that passed at once were checked by breaking the code on purpose.
- **Coverage gate**: `npm run coverage` fails below 80%. Currently about 98% of lines and 95% of branches are covered (test helpers and the entry file excluded).
- **Outside-in**: each increment began with a flow test of the whole user journey, which turned green only when all the pieces were in.
- **Tests like a user**: elements are found by role and accessible name (`getByRole('button', { name: 'Add to cart' })`), not CSS classes, so tests survive restyling and double as accessibility checks.
- **Realistic fake backend (MSW)**: tests run against fake API answers in the same shape as the real API (including 401, 404 and 409 error bodies); a request without a fake answer fails the test.
- **One end-to-end journey against the real backend** (Playwright): register → search → add to cart → change quantity → checkout → "My orders", then checks the real stock went down. It creates its own user and picks a book with stock left, so it can run again and again against the same backend.
- **Stock race end to end** (Playwright): a customer has the last copy in the cart, another customer buys it first; the cart then flags it as sold out and blocks checkout, and the catalog shows "Out of stock".
- **The real route table in tests**: flow tests render the same routes the browser gets, inside a memory router.

**Architecture**
- **Feature-based folders** with one public `index.ts` per feature; features combined in the router instead of importing each other; enforced by ESLint.
- **Server state in TanStack Query, client state in React context**: one cached cart shared by the badge and the cart page.
- **The backend is the source of truth**: totals, stock warnings and "can check out" come from the API; the frontend decides only how to show them.
- **One HTTP client** for every call, one `ApiError` type for every failure; components never call `fetch` directly.
- **Configuration in `.env` files**, read in one place (`lib/env.ts`) with a clear error when missing; shared paths as constants (`HOME_PATH`).

**Code quality**
- **TypeScript strict mode**, `@/` imports instead of `../../`, ESLint with React, hooks and TypeScript rules.
- **Consistent formatting**: Prettier formats every file the same way; `npm run format:check` fails on unformatted files, so style never comes up in reviews.
- **Small, focused components**: presentational ones (props in, events out) separate from connected ones (using hooks).
