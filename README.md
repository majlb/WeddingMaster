# Wedmaster

Wedmaster is a wedding-planning platform starter with a React/Vite client and a Java 17/Spring Boot API. It includes a responsive, multilingual planning dashboard; a PostgreSQL-backed API; role-based customer, supplier, and administrator workflows; and Stripe/WebSocket integration points.

## What is included

- **React:** responsive customer dashboard, supplier discovery and filters, favourites, task checklist with per-item budgets and a total, couple-selectable USD/EUR planning currency (USD by default), invitation template designer with direct WhatsApp card delivery, AI wedding-planning assistant, booking enquiry modal, customer/supplier account registration and sign-in, supplier profile editor, admin category/approval dashboard, English/French/Greek/Arabic UI, RTL layout, and dark mode.
- **Spring Boot:** JWT authentication, customer and pending-supplier registration, supplier categories and public profiles, owner profile editing, booking requests and supplier quotes, booking confirmation, checklist synchronization, reviews, media uploads, booking chat and persistent real-time in-app notifications over authenticated STOMP/WebSockets, Twilio WhatsApp invitation-image delivery, configurable Ollama/OpenRouter wedding assistant, payment intents/refunds/webhook processing, and admin category/supplier approval endpoints.
- **Database:** PostgreSQL schema managed by Flyway. H2 in-memory is configured for local API development.
- **Email:** optional booking-request, quote, and confirmation messages sent after database commits when `MAIL_ENABLED=true` and SMTP is configured.
- **WhatsApp:** optional Twilio WhatsApp delivery for in-app notifications when `WHATSAPP_ENABLED=true`; users can add an E.164 phone number during registration or in their account profile.
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
4. Configure WhatsApp notifications with `WHATSAPP_ENABLED=true`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM`, and `TWILIO_WHATSAPP_CONTENT_SID`. Create and approve a Twilio WhatsApp content template containing `{{1}} {{2}} {{3}}. Open Wedora to view the details.`; the variables are the actor, notification action, and event context.
5. To deliver invitation cards directly, also set `TWILIO_WHATSAPP_INVITATION_CONTENT_SID` to an approved image-header template and `PUBLIC_API_URL` to the API's public HTTPS origin. The template's image header URL should be `https://<api-origin>/api/guest-invitations/media/{{1}}` and its body should include `{{2}}`. Guests must have opted in and use international E.164 phone numbers. See [the invitation API notes](docs/API.md#authentication) for details.

6. To use local Ollama for the assistant, install [Ollama](https://ollama.com/) on the API host, download a local model, and start its service:

   ```powershell
   ollama pull llama3.2:3b
   ollama list
   $env:AI_PROVIDER = "ollama"
   $env:OLLAMA_BASE_URL = "http://localhost:11434"
   $env:OLLAMA_MODEL = "llama3.2:3b"
   $env:OLLAMA_TIMEOUT_SECONDS = "180"
   mvn spring-boot:run
   ```

   The backend defaults to OpenRouter. Set `AI_PROVIDER=openrouter` and inject `OPENROUTER_API_KEY` as a backend process/deployment secret. Do not put the key in frontend `VITE_` variables, `.env`, or source control. The default model is `openai/gpt-4o-mini`; override it with `OPENROUTER_MODEL` if needed. `OPENROUTER_BASE_URL` and `OPENROUTER_TIMEOUT_SECONDS` are also configurable. Recent chat messages and up to 25 approved public supplier summaries are sent to the configured provider; Wedmaster does not persist assistant conversations. OpenRouter is an external service and may process data according to its own policies.

   For Render, add `AI_PROVIDER`, `OPENROUTER_API_KEY`, and optionally `OPENROUTER_MODEL` in the **backend web service** environment, then redeploy. `render.yaml` configures only the frontend, and Docker Compose settings do not automatically apply to a separate Render service. Use an OpenRouter key, not an OpenAI key. If chat returns 503, inspect the backend log for `Configured assistant model request failed`; a provider HTTP status of 401/403 indicates credentials or access, 402 indicates credits, and 429 indicates rate limiting. The logged status is the provider's status, not the application's generic 503. Check the key, OpenRouter balance, model access, or provider availability as appropriate. Never share keys or chat contents in logs or support requests.

## Authentication and authorization

- `POST /api/auth/register` creates a customer account.
- `POST /api/auth/register-supplier` creates a supplier account and an unapproved business profile.
- `POST /api/auth/login` returns a signed JWT. Send it as `Authorization: Bearer <token>`.
- New supplier listings remain private until an administrator approves them.
- Supplier profile edits and booking responses are restricted to the owning supplier; booking confirmations and reviews are restricted to the customer.
- Create the first administrator through a controlled database provisioning process by promoting a trusted account's `role` to `ADMIN`. Do not expose public administrator registration.
- Replace `JWT_SECRET` with a unique, high-entropy secret before deploying.

## Docker deployment

Copy `.env.example` to `.env`, set a unique `JWT_SECRET` and database password, and configure Stripe, AI provider/model, and mail values as needed. For OpenRouter, set `AI_PROVIDER=openrouter` and inject `OPENROUTER_API_KEY` from a secret manager or the shell environment used to launch Docker Compose; Compose forwards it only to the backend. Do not store the key in `.env`. Then run:

```powershell
docker compose up --build -d
```

The first startup downloads the configured model into the Ollama container before starting the API, which can take several minutes, even when OpenRouter is selected. The client is served at `http://localhost:8081`; it proxies `/api`, `/ws`, and `/uploads` to the API. With `AI_PROVIDER=ollama`, inference stays local and no provider key is needed. With `AI_PROVIDER=openrouter` (the Compose default), chat messages are sent to OpenRouter and its selected model; provide the API key only to the backend. PostgreSQL data, model files, and uploads persist in named volumes. The production API profile validates Flyway-managed schema instead of applying automatic Hibernate updates. Back up these volumes and use a TLS-terminating reverse proxy for public deployments.

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
