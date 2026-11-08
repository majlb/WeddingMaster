# Wedmaster

Wedmaster is a wedding-planning platform starter with a React/Vite client and a Java 17/Spring Boot API. It includes a responsive, multilingual planning dashboard; a PostgreSQL-backed API; role-based customer, supplier, and administrator workflows; and Stripe/WebSocket integration points.

## What is included

- **React:** responsive customer dashboard, supplier discovery and filters, favourites, task checklist with per-item budgets and a total, couple-selectable USD/EUR planning currency (USD by default), AI wedding-planning assistant, booking enquiry modal, customer/supplier account registration and sign-in, supplier profile editor, admin category/approval dashboard, English/French/Greek/Arabic UI, RTL layout, and dark mode.
- **Spring Boot:** JWT authentication, customer and pending-supplier registration, supplier categories and public profiles, owner profile editing, booking requests and supplier quotes, booking confirmation, checklist synchronization, reviews, media uploads, booking chat and persistent real-time in-app notifications over authenticated STOMP/WebSockets, local Ollama wedding assistant, payment intents/refunds/webhook processing, and admin category/supplier approval endpoints.
- **Database:** PostgreSQL schema managed by Flyway. H2 in-memory is configured for local API development.
- **Email:** optional booking-request, quote, and confirmation messages sent after database commits when `MAIL_ENABLED=true` and SMTP is configured.
- **API reference:** OpenAPI/Swagger UI at `/swagger-ui/index.html` while the API is running.
- **Deployment:** Docker Compose, production Spring profile, health endpoint, and GitHub Actions CI.

Supplier categories are database records, not a closed enum. Venues, catering, flowers, dresses, hair, makeup, and photography are added at startup; admins can add or archive categories through the API.

## Run locally

Requirements: Java 17, Maven 3.9+, Node.js 20+, and npm.

1. Start the API:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

   The API uses an in-memory H2 database by default at `http://localhost:8080`. Initial categories are seeded automatically. Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` to use PostgreSQL instead.

   When running the Vite development server at `http://localhost:5173`, the non-production API profile allows that local origin for CORS. Production continues to allow only the configured `FRONTEND_URL`.
   Vite proxies WebSocket chat connections through `/ws`; keep the frontend and API on their documented local ports when using the default configuration.

2. In another terminal, start the frontend:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`. The dashboard includes sample supplier cards for preview; supplier searches use live API data when approved listings exist. Create a customer account from the profile menu to use authenticated booking and planning APIs.

3. Configure online payments by setting `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` on the API, and set `VITE_STRIPE_PUBLISHABLE_KEY` when building a Stripe-enabled frontend. The backend returns an explicit `503` until its Stripe secret is configured.

4. Install [Ollama](https://ollama.com/) on the API host, download a local model, and start its service:

   ```powershell
   ollama pull llama3.2:3b
   ollama list
   $env:OLLAMA_BASE_URL = "http://localhost:11434"
   $env:OLLAMA_MODEL = "llama3.2:3b"
   $env:OLLAMA_TIMEOUT_SECONDS = "180"
   mvn spring-boot:run
   ```

   If `ollama pull` or `ollama list` cannot connect, start the Ollama application/service first (or run `ollama serve` in another terminal). The assistant uses LangChain4j's Ollama integration and does not send prompts to OpenAI or another external AI provider. Configure `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, and `OLLAMA_TIMEOUT_SECONDS` in the API environment as needed; the default model timeout is 180 seconds to accommodate slower local inference. Recent chat messages and up to 25 approved public supplier summaries are processed by that Ollama instance; Wedmaster does not persist assistant conversations.

## Authentication and authorization

- `POST /api/auth/register` creates a customer account.
- `POST /api/auth/register-supplier` creates a supplier account and an unapproved business profile.
- `POST /api/auth/login` returns a signed JWT. Send it as `Authorization: Bearer <token>`.
- New supplier listings remain private until an administrator approves them.
- Supplier profile edits and booking responses are restricted to the owning supplier; booking confirmations and reviews are restricted to the customer.
- Create the first administrator through a controlled database provisioning process by promoting a trusted account's `role` to `ADMIN`. Do not expose public administrator registration.
- Replace `JWT_SECRET` with a unique, high-entropy secret before deploying.

## Docker deployment

Copy `.env.example` to `.env`, set a unique `JWT_SECRET` and database password, and configure Stripe, Ollama model, and mail values as needed. Then run:

```powershell
docker compose up --build -d
```

The first startup downloads the configured model into the Ollama container before starting the API, which can take several minutes. The client is served at `http://localhost:8081`; it proxies `/api`, `/ws`, and `/uploads` to the API. Ollama and its downloaded models run locally in a separate container; no AI provider key is needed. PostgreSQL data, model files, and uploads persist in named volumes. The production API profile validates Flyway-managed schema instead of applying automatic Hibernate updates. Back up these volumes and use a TLS-terminating reverse proxy for public deployments.

## Render frontend deployment

For a Render Node web service, set the root directory to `frontend`, the build command to `npm install && npm run build`, and the start command to `npm start`. The start script serves the production Vite build on Render's `PORT`; unlike the local Vite development server, it does not proxy `/api` requests. Set `VITE_API_URL` on the frontend service to `https://weddingmaster.onrender.com/api` and `VITE_WS_URL` to `wss://weddingmaster.onrender.com/ws`. Production builds default to this API URL if `VITE_API_URL` is unset; make sure no Render build environment setting overrides it with `/api`. These `VITE_` values are embedded at build time, so redeploy the frontend after changing them. Set the backend's `FRONTEND_URL` to the frontend's exact public origin (for example, `https://weddingmaster-web.onrender.com`) so browser API requests pass CORS. To verify the API configuration, open `https://weddingmaster.onrender.com/api/countries` directly; it should return a JSON array. Alternatively, deploy the frontend as a Render Static Site with `npm install && npm run build` and `dist` as the publish directory.

## CI

GitHub Actions runs Maven tests and the Vite production build on pushes and pull requests. The Dockerfiles can be used by a release pipeline to publish the backend and frontend images; configure registry credentials and deployment environments in repository secrets rather than source files.

## Important production follow-ups

This repository is a functional starter, not a fully operated marketplace. Before accepting real customers, configure and test Stripe webhooks/refunds, SMTP delivery, TLS, durable/object storage for media, database backups, rate limiting, GDPR export/deletion and retention workflows, moderation/dispute handling, and an administrator provisioning runbook. Availability is currently stored as supplier-edited JSON text; scheduling conflict checks and calendar integrations are not yet implemented. The dashboard's example content is presentation data, not a seeded marketplace.

## Project layout

```text
backend/
  src/main/java/com/wedora/       Spring Boot domain, repositories, services, controllers
  src/main/resources/db/migration Flyway PostgreSQL schema
frontend/
  src/App.jsx                     React planning dashboard and example components
  src/api.js                      Axios API client
  src/i18n.js                     English, French, Greek, and Arabic resources
  src/styles.css                  Responsive light/dark and RTL styles
docs/
  API.md                          API routes and booking/payment/chat flows
```
