# YourBrand — Phase 0 landing page

Pre-launch landing page for a Bangladesh-first, multi-model AI workspace. Its job is to validate interest through
an early-access waitlist. It is **not** the AI product: there is no chat, billing or checkout.

> "YourBrand" is a temporary brand token. Change it in `src/lib/site.ts`, `src/components/landing/Logo.tsx` and `src/app/icon.svg`.

## Stack

Next.js 16 (App Router, Turbopack) · React 19 · TypeScript · Tailwind CSS v4 · Zod · Vitest + Testing Library

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
| `NEXT_PUBLIC_SITE_URL`      | Prod     | Base URL for canonical, Open Graph, robots.txt and sitemap.  |
| `NEXT_PUBLIC_CONTACT_EMAIL` | Prod     | Contact / data-deletion address in the footer and privacy page. |

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

- Pricing section and preference buttons (deferred; the survey still asks about pricing preference)
- Bangla copy (the font stack already falls back to Noto Sans Bengali)
- Unsubscribe and deletion endpoints (handled manually via the contact email for now)
