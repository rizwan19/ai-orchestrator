# YourBrand — Phase 0 landing page

Pre-launch landing page for a Bangladesh-first, multi-model AI workspace, plus an early chat prototype at `/chat`.
The landing page validates interest through an early-access waitlist and does not link to the chat.

> "YourBrand" is a temporary brand token. Change it in `src/lib/site.ts`, `src/components/landing/Logo.tsx` and `src/app/icon.svg`.

## Stack

- **Frontend:** Next.js 16 (App Router, Turbopack) · React 19 · TypeScript · Tailwind CSS v4 · Zod · Vitest + Testing Library
- **Chat backend** (`backend/`): Spring Boot 4.1 · Java 25 · Spring Security · JPA + Flyway · PostgreSQL 18 · JUnit + Testcontainers

## Getting started

```bash
cp .env.example .env.local
nvm use              # Node >= 20.9 (see .nvmrc)
npm install
npm run dev          # http://localhost:3000
```

Or with Gradle. Only Java is required: Gradle downloads a pinned Node.js 22 into `.gradle/nodejs/` and runs `npm ci` when the lockfile changes, so the system `node` version does not matter. This is useful from IntelliJ, where nvm isn't loaded.

```bash
./gradlew dev                # dev server on :3000 (add -Pport=4000 to change it)
./gradlew start              # production build + serve
./gradlew test               # tests; also: lint, nextBuild, npmInstall
```

| Script              | Purpose                                 |
| ------------------- | --------------------------------------- |
| `npm run dev`       | Development server                      |
| `npm run build`     | Production build                        |
| `npm start`         | Serve the production build              |
| `npm test`          | Unit + component + route handler tests  |
| `npm run lint`      | ESLint                                  |
| `npm run typecheck` | Generate route types and run `tsc`      |

### Environment variables

| Variable                    | Required | Description                                                  |
| --------------------------- | -------- | ------------------------------------------------------------ |
| `BACKEND_URL`               | No       | Where `/api/backend/*` is proxied. Default `http://localhost:8080`. |
| `NEXT_PUBLIC_SITE_URL`      | Prod     | Base URL for canonical, Open Graph, robots.txt and sitemap.  |
| `NEXT_PUBLIC_CONTACT_EMAIL` | Prod     | Contact / data-deletion address in the footer and privacy page. |

## Chat (`/chat`)

Signed-in users chat with a curated set of low-cost models through [OpenRouter](https://openrouter.ai). Replies
stream in token by token, and conversations are saved in Postgres.

```
browser ──/api/backend/*──▶ Next.js rewrite ──▶ Spring Boot :8080 ──▶ OpenRouter
                                                      │
                                                  PostgreSQL
```

The browser only talks to Next.js, so session and CSRF cookies are same-origin and need no CORS setup.
The OpenRouter key never leaves the backend.

### Running it

Requires Docker (for Postgres) and JDK 25. Gradle finds a local JDK automatically, for example from SDKMAN.

```bash
cp backend/.env.example backend/.env   # then put your key in OPENROUTER_API_KEY (git-ignored)
./gradlew devAll                       # backend + frontend together; open http://localhost:3000/signup
```

`devAll` runs Postgres (through `backend/compose.yaml`), Spring Boot on :8080, and Next.js on :3000 in parallel,
with log lines prefixed `[backend]` or `[frontend]`. Ctrl+C stops both servers. Spring Boot usually stops Postgres too; if it's
still up, run `docker compose -f backend/compose.yaml down` (data is kept in a volume; add `-v` to wipe it). Use `-Pport=…` to change the frontend port. You can also run each
server on its own with `./gradlew :backend:bootRun` and `./gradlew dev`.

`./gradlew test` runs both test suites: Vitest, and the backend's JUnit tests, which start a throwaway Postgres
through Testcontainers.

### Configuration (`backend/src/main/resources/application.properties`)

| Property                      | Default            | Meaning |
| ----------------------------- | ------------------ | ------- |
| `app.chat.models[n]`          | Haiku 5.5, GPT-6 Luna, DeepSeek V4.1 Flash | Allowed OpenRouter model IDs; the first is the default. |
| `app.chat.daily-budget-usd`   | `0.50`             | Per-user spend cap; requests are refused with 429 once reached. |
| `app.chat.budget-zone`        | `Asia/Dhaka`       | Time zone in which the daily budget resets. |
| `app.chat.history-limit`      | `30`               | Earlier messages sent as context with each request. |
| `app.chat.max-message-length` | `8000`             | Maximum characters per user message. |

Any property can be overridden with an environment variable, e.g. `APP_CHAT_DAILY_BUDGET_USD=1.00`.

### API

All endpoints are under `/api` and require a session, except `auth/csrf`, `auth/login` and `auth/signup`. Mutating
requests need the `X-XSRF-TOKEN` header, whose value comes from the `XSRF-TOKEN` cookie. Errors are RFC 9457 problem
details (`{ "detail": "…" }`).

| Method & path                        | Purpose |
| ------------------------------------ | ------- |
| `GET auth/csrf`                      | Issue the CSRF cookie |
| `POST auth/signup` / `auth/login`    | `{ email, password }` → user, starts a session (409 duplicate / 401 bad credentials) |
| `POST auth/logout` · `GET auth/me`   | End session · current user |
| `GET models` · `GET usage/today`     | Curated models + default · `{ spentUsd, budgetUsd }` |
| `GET/POST conversations`             | List · create `{ model? }` |
| `PATCH/DELETE conversations/{id}`    | Rename `{ title }` · delete |
| `GET conversations/{id}/messages`    | History |
| `POST conversations/{id}/messages`   | `{ content, model }` → SSE stream: `delta {text}`… then `done {message, usage}` or `error {message}` |

### Known limitations

- **Cost of stopped replies is not recorded.** When a user presses Stop, the partial reply is saved, but OpenRouter's
  usage report never arrives, so that spend doesn't count toward the daily budget. OpenRouter's
  `GET /api/v1/generation?id=` could recover it.
- **No rate limiting on login or sign-up, and no email verification or password reset.** Add these before
  exposing open sign-up publicly.
- Cost is OpenRouter "credits", treated as USD.
- Sessions are stored in server memory, so restarting the backend logs everyone out. Use Spring Session JDBC for
  persistent sessions.

## Project structure

```
src/app/
  page.tsx, layout.tsx            Landing page + metadata (title, description, OG, canonical)
  privacy/page.tsx                Privacy policy (placeholder — needs legal review)
  api/v1/waitlist/route.ts        POST   — register email
  api/v1/waitlist/[registrationId]/preferences/route.ts  PATCH — optional survey
  robots.ts, sitemap.ts, icon.svg, opengraph-image.tsx
src/components/landing/           Navbar, Hero, ProductPreview, ProblemSection, FeatureGrid,
                                  HowItWorks, AgentShowcase, FAQ, WaitlistSection, WaitlistForm,
                                  WaitlistSurvey, Footer, LandingAnalytics
src/components/ui/                Button, Section/Container, Tag
src/lib/
  validation.ts                   Zod schemas shared by client and server
  db.ts                           WaitlistStore interface + in-memory dev adapter
  rate-limit.ts                   In-memory fixed-window limiter
  analytics.ts                    Typed, provider-agnostic `track()` stub
  api.ts                          Client fetch helpers
```

## Waitlist API

`POST /api/v1/waitlist`

```json
{ "email": "example@email.com", "source": "landing_page",
  "utm": { "source": "facebook", "medium": "social", "campaign": "beta" },
  "marketingConsent": true }
```

→ `201` new · `200` already registered (same `registrationId`, nothing else exposed) · `400` invalid · `429` throttled · `500` storage failure.
Response: `{ "registrationId": "…", "persisted": false }`.

`PATCH /api/v1/waitlist/{registrationId}/preferences`. Every field is optional:
`persona` (student|professional|business|other), `primaryInterest` (models|credits|agents),
`monthlyBudgetRange` (free|49-149|150-399|400+), `pricingPreference` (prepaid|subscription|unsure).
→ `200` · `400` · `404` · `429` · `500`.

Spam and abuse protection: a honeypot field (`website`), input length limits, and per-IP rate limiting (5 sign-ups/min, 20 survey updates/min).
Server logs never include email addresses.

## ⚠️ Storage: development fallback only

`src/lib/db.ts` ships an **in-memory** store. Sign-ups are lost when the server restarts, and they are not shared
between instances. The API returns `persisted: false`, and the success message says so, so the page never claims
a sign-up was saved when it wasn't.

Before launch, implement `WaitlistStore` against a durable database and set `persistent: true`. Suggested Postgres schema:

```sql
create table waitlist_registrations (
  id                uuid primary key default gen_random_uuid(),
  email             text not null,
  source            text not null,
  utm_source        text, utm_medium text, utm_campaign text,
  marketing_consent boolean not null default false,
  created_at        timestamptz not null default now()
);
create unique index waitlist_email_unique on waitlist_registrations (lower(email));

create table waitlist_preferences (
  registration_id      uuid primary key references waitlist_registrations(id) on delete cascade,
  persona              text check (persona in ('student','professional','business','other')),
  primary_interest     text check (primary_interest in ('models','credits','agents')),
  monthly_budget_range text check (monthly_budget_range in ('free','49-149','150-399','400+')),
  pricing_preference   text check (pricing_preference in ('prepaid','subscription','unsure')),
  updated_at           timestamptz not null default now()
);
```

Use `insert … on conflict (lower(email)) do nothing returning …` for idempotent sign-ups, and
`insert … on conflict (registration_id) do update` to upsert preferences. Also move the rate limiter to a shared
store (e.g. Redis) once you run more than one instance.

## Analytics

`track()` in `src/lib/analytics.ts` logs events to the console in development and does nothing in production.
Connect a provider there. Events: `landing_view`, `waitlist_started`, `waitlist_submitted` (only after the server
confirms the sign-up), `persona_selected`, `agent_interest_selected`, `pricing_preference_selected`. No event
carries the email address. Without third-party scripts, no cookie banner is needed. Revisit this if you add a
provider that sets cookies.

## Deployment

Any Node host that runs Next.js works (Vercel, Render, Fly, or a container with `npm run build && npm start`).
Set `NEXT_PUBLIC_SITE_URL` and `NEXT_PUBLIC_CONTACT_EMAIL`, and replace the in-memory store first. A serverless
host makes in-memory storage even less reliable, because each instance has its own memory.

## Not in scope (yet)

- Billing, credit purchases, file attachments

- Pricing section and preference buttons (deferred; the survey still asks about pricing preference)
- Bangla copy (the font stack already falls back to Noto Sans Bengali)
- Unsubscribe and deletion endpoints (handled manually via the contact email for now)
