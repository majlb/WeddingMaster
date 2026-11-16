# Wedora API

Base URL: `http://localhost:8080/api`. JSON responses use Spring Problem Details for API and validation errors. The full OpenAPI definition is available at `/v3/api-docs`; Swagger UI is at `/swagger-ui/index.html`.

Except for registration/login, public supplier/category reads, health, and the Stripe webhook, requests require `Authorization: Bearer <JWT>`. Role names are `CUSTOMER`, `SUPPLIER`, and `ADMIN`.

## Authentication

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Create customer; `{ "name", "username", "password", "countryCodes": ["FR", "GR"] }`; email is not collected and usernames are unique case-insensitively |
| POST | `/auth/register-supplier` | Public | Create a supplier application using `username`, `businessName`, `location`, `countryCodes`, and optional `description`; email is not collected, usernames are unique case-insensitively, and categories are chosen later in the supplier dashboard |
| GET | `/auth/username-availability?username=` | Public | Check whether a valid username is available |
| POST | `/auth/login` | Public | Exchange `{ "username": "name", "password": "..." }` for an auth response including `token`, `id`, `name`, and `username`; legacy accounts may still sign in using their email as the username value |
| PUT | `/users/me/countries` | Couple | Update the signed-in couple's country preferences |
| PUT | `/users/me/wedding-date` | Couple | Save or clear the couple's wedding date using `{ "weddingDate": "YYYY-MM-DD" }` (or `null`) |
| PUT | `/users/me/phone-number` | Authenticated user | Save or clear the user's WhatsApp number using `{ "phoneNumber": "+15551234567" }` (or `null`); numbers must use E.164 format |
| POST | `/guests/{id}/invitation` | Couple who owns the guest | Send a generated PNG card to the guest with the configured Twilio WhatsApp image template; multipart fields are `card` (PNG, max 5 MB) and `message` (1–1,600 characters) |
| GET | `/guest-invitations/media/{uuid}` | Public, unguessable temporary link | Serve the card image to Twilio; links expire after 24 hours and are cleaned up |
| POST | `/assistant/chat` | Couple | Generate a wedding-planning reply using up to 12 alternating `{ "role": "user"|"assistant", "content": "..." }` messages; each message is limited to 1,200 characters |
| GET | `/notifications` | Authenticated user | Return the 50 newest in-app notifications and the total unread count |
| PUT | `/notifications/{id}/read` | Authenticated user | Mark one of the user's notifications as read |
| PUT | `/notifications/read-all` | Authenticated user | Mark all of the user's notifications as read |

Passwords must be 10–72 characters and are stored using BCrypt. Tokens expire after the configured `JWT_EXPIRATION_MS` (default 24 hours).
Usernames are 3–30 letters, numbers, periods, underscores, or hyphens. New accounts do not require or publish an email address; existing accounts retain email sign-in.
The wedding date is included in authentication responses and is stored on the couple's account.

Direct WhatsApp invitation delivery requires `WHATSAPP_ENABLED=true`, the existing Twilio account/sender credentials, `TWILIO_WHATSAPP_INVITATION_CONTENT_SID` set to an approved WhatsApp image-header template, and `PUBLIC_API_URL` set to the API's public HTTPS origin (without a path). Configure the template image URL as `https://<api-origin>/api/guest-invitations/media/{{1}}` and its body to include `{{2}}`; the API supplies the temporary image token and personalized invitation message as those variables. Guests' phone numbers must be in international E.164 format, recipients must have opted in, and WhatsApp template approval and messaging rules apply. Invitation images are stored outside the general public uploads directory and removed after 24 hours.

## Suppliers and categories

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | `/countries` | Public | ISO country codes and names for multi-country selectors |
| GET | `/categories` | Public | Active categories |
| GET | `/suppliers?category=&location=&maxPrice=&minRating=` | Public | Approved suppliers and filters |
| GET | `/suppliers/{id}` | Public | Approved supplier details, including public contact/social links and map coordinates when set |
| GET | `/suppliers/me` | Supplier | List the signed-in supplier's profiles, including served country codes |
| GET | `/suppliers/me/{id}/analytics` | Owning supplier | Profile visits, booking count, and message count |
| PUT | `/suppliers/{id}` | Owning supplier | Edit business profile, services, availability, pricing, served country codes, and optional map coordinates (`latitude`, `longitude`). Public contact fields are `contactEmail`, `contactPhone`, `whatsapp`, `website`, `facebook`, `instagram`, and `tiktok`; they are optional and separate from the required private account email. |
| PUT | `/suppliers/{id}/categories` | Owning supplier | Replace work categories using `{ "categoryIds": [id, ...] }` |
| GET | `/suppliers/{id}/reviews` | Public | Supplier reviews |
| GET | `/suppliers/{id}/media` | Public | Approved supplier's image/video/PDF gallery |
| POST | `/suppliers/{id}/media` | Owning supplier | Multipart image/video upload (`file`, optional `caption`); max 50 MB |
| POST | `/suppliers/{id}/media/main-image` | Owning supplier | Multipart JPEG/PNG/WebP upload (`file`) used as the supplier's search-card image; max 50 MB |
| GET | `/suppliers/{id}/services` | Public | Approved supplier's service offerings |
| GET | `/suppliers/me/{id}/services` | Owning supplier | List the supplier's service offerings, including while awaiting approval |
| POST | `/suppliers/{id}/services` | Owning supplier | Multipart service (`service` JSON part with `categoryId`, `translations` for `en`, `fr`, `el`, and `ar`, optional `price`; optional `image`) |
| PUT | `/suppliers/{id}/services/{serviceId}` | Owning supplier | Update the category, all language translations, and optionally replace its image |
| DELETE | `/suppliers/{id}/services/{serviceId}` | Owning supplier | Delete an offering that has no booking history |
| GET | `/suppliers/{id}/services/{serviceId}/availability?from=YYYY-MM-DD&to=YYYY-MM-DD` | Public | List manually blocked full-day dates for an approved supplier option (maximum 12-month range) |
| GET | `/suppliers/me/{id}/services/{serviceId}/availability?from=YYYY-MM-DD&to=YYYY-MM-DD` | Owning supplier | List the supplier option's blocked dates |
| GET | `/suppliers/me/{id}/service-bookings` | Owning supplier | List all service bookings with customer name, date, status, service ID and `dateBlocked` |
| POST | `/suppliers/me/{id}/services/{serviceId}/availability` | Owning supplier | Block a date using `{ "date": "YYYY-MM-DD" }` |
| DELETE | `/suppliers/me/{id}/services/{serviceId}/availability/{date}` | Owning supplier | Remove a manual date block |

Supplier discovery accepts category slug, partial location, maximum starting price, and minimum rating. Suppliers choose one or more work categories in the dashboard, then assign each service to one selected category. Each service has its own all-day date availability calendar. Suppliers choose a precise location by placing or dragging a pin on the map in their business profile; coordinates are stored as `latitude` and `longitude` and shown on the public supplier page. Map tiles are provided by OpenStreetMap.
Service bookings never automatically block a date. Suppliers can block or unblock dates manually, including dates with existing bookings. The dashboard warns for active bookings whose date is not blocked; multiple bookings can exist for the same date. Blocks prevent new requests but do not prevent accepting or confirming requests already received. The manual-availability migration removes existing booking-created blocks while preserving manually created blocks.
An administrator can approve a supplier only after at least one work category has been selected.

Category names/descriptions, supplier business-profile text, and individual service names/descriptions support English, French, Greek, and Arabic translations. API responses select the requested language from the `Accept-Language` header and fall back to the English value when a supplier translation is not available. Admin category create/update requests include a `translations` object keyed by language (`en`, `fr`, `el`, `ar`), each with translated `name` and `description`; supplier profile updates accept `localizedContent` keyed by language with `businessName`, `location`, `description`, `services`, and `availability`. Service create/update requests include a `translations` object with a `name` and optional `description` for each of the four languages. Couple-written booking notes, checklist labels, reviews, and chat messages remain in the language they were entered.

Service create/update requests and responses also include optional `pricingType` (`PERSON`, `TABLE`, or `WEDDING`) and integer `minimumAmount` and `maximumAmount` quantities. Quantities default to 1 for new and existing services; they must be positive and the maximum cannot be less than the minimum. Omitted quantities retain their existing values on update. These limits describe the offering and do not enforce a booking quantity.

## Supplier collaborations

Suppliers can create collaborations with names translated into English, French, Greek, and Arabic, and invite one or more approved suppliers at creation. Invitees can accept or decline, and each accepted supplier can add their own existing service offerings. Only the creator can activate a collaboration, and activation requires every invited supplier to have accepted and added at least one service. Active collaborations appear to couples, who can select or unselect offers for their wedding.

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | `/collaborations` | Supplier | Create with `{ "supplierId", "names": { "en", "fr", "el", "ar" }, "invitedSupplierIds": [id, ...], "serviceIds": [id, ...] }`; the creator's selected services are included immediately, the creator joins as accepted, and invitees are pending |
| GET | `/collaborations` | Supplier | List collaborations and invitations for the signed-in supplier |
| POST | `/collaborations/{id}/invitations` | Collaboration creator | Invite an additional approved supplier using `{ "supplierId" }` |
| DELETE | `/collaborations/{id}/members/{supplierId}` | Collaboration creator | Remove a pending or declined invitation before activation |
| POST | `/collaborations/{id}/response` | Invited supplier | Accept or decline using `{ "accepted": true|false }` |
| POST | `/collaborations/{id}/items` | Accepted collaboration supplier | Add one of that supplier's existing offerings using `{ "serviceId" }` |
| POST | `/collaborations/{id}/items/batch` | Accepted collaboration supplier | Add one or more of that supplier's existing services using `{ "serviceIds": [id, ...] }` |
| POST | `/collaborations/{id}/activate` | Collaboration creator | Activate after all invited suppliers accepted and each added at least one service |
| GET | `/collaborations/public` | Couple | List active offers and indicate which offers this couple selected |
| POST | `/collaborations/{id}/select` | Couple | Select an active collaboration for the wedding |
| DELETE | `/collaborations/{id}/select` | Couple | Remove the selection |

Couple and supplier registration requests include one or more `countryCodes` values using ISO 3166-1 alpha-2 codes. Couples can update country preferences with `PUT /users/me/countries`; supplier profiles can update the countries they serve with `PUT /suppliers/{id}`. Couple discovery and saved suppliers show only suppliers serving at least one selected country.

The couple-only `POST /api/assistant/chat` endpoint uses the backend AI provider configured by `AI_PROVIDER` (`openrouter` by default, or `ollama`). For Ollama, install it on the API host and download a model (for example, `ollama pull llama3.2:3b`); configure `OLLAMA_BASE_URL` (default `http://localhost:11434`), `OLLAMA_MODEL` (default `llama3.2:3b`), and `OLLAMA_TIMEOUT_SECONDS` (default `180`). For OpenRouter, provide `OPENROUTER_API_KEY` only to the backend, and optionally configure `OPENROUTER_MODEL` (default `openai/gpt-4o-mini`), `OPENROUTER_BASE_URL` (default `https://openrouter.ai/api/v1`), and `OPENROUTER_TIMEOUT_SECONDS` (default `60`). Provider HTTP failures are logged by status without logging the response body; 401/403, 402 and 429 return the respective codes `assistant.provider_auth`, `assistant.provider_credit` and `assistant.provider_rate_limit` with HTTP 503. Recent chat messages and up to 25 approved public supplier summaries in the couple's selected countries are sent to the configured provider. OpenRouter is external and may process data under its own policies. Conversations are not stored by Wedmaster. Do not include sensitive personal or payment information in assistant messages.

## Bookings and checklist

1. A customer sends `POST /bookings` with `{ "supplierId", "serviceOfferingId", "eventDate", "notes" }`; `serviceOfferingId` is optional for legacy/general enquiries. A linked checklist task is created as pending. The selected option must belong to the requested supplier and its date must not be manually blocked.
2. The owning supplier can accept a request with `POST /bookings/{id}/accept`, decline it with `POST /bookings/{id}/decline`, or send an offer using `POST /bookings/{id}/quote` with `{ "amountCents", "currency" }`. An accepted request can still be quoted later. These transitions do not modify manually blocked dates.
3. The customer accepts a quote with `POST /bookings/{id}/confirm`. This confirms the booking and completes its linked checklist task.
4. The supplier can decline a request, accepted request, or quote with `POST /bookings/{id}/decline`.
5. After the event, the owning supplier can call `POST /bookings/{id}/complete`.
6. `GET /bookings` lists the current user's bookings. A supplier sees bookings for their own profiles. Responses include the customer name, event date, notes, status, quote details, and the localized selected option name when applicable.
7. Couples can set their preferred `USD` or `EUR` currency with `PUT /api/users/me/currency` and `{ "currencyCode": "USD" }`; new accounts default to USD. `GET /checklist`, `POST /checklist` (with optional `categorySlug` and `budgetAmount` in the couple's selected currency), `PUT /checklist/{id}` (to update the title, category, due date, notes, and budget), `PATCH /checklist/{id}?completed=true|false`, and `DELETE /checklist/{id}` let a customer manage their own checklist. Checklist changes do not alter the associated booking. The client shows the sum of item budgets in the selected currency. Changing the preference changes display only and does not convert saved amounts.

Reviews can be submitted using `POST /bookings/{id}/reviews` with `{ "rating": 1..5, "comment": "..." }` once a booking is marked `COMPLETED`.

## Payments and invoices

- `POST /bookings/{id}/payments` creates a Stripe PaymentIntent for a confirmed, quoted booking and returns a client secret for Stripe Elements.
- `GET /payments` returns the signed-in customer's payment history.
- Stripe posts signed events to `POST /payments/webhook` using `Stripe-Signature`. Configure both Stripe secrets before enabling this route.
- `POST /admin/payments/{id}/refund` submits a full Stripe refund; the payment record is updated from webhook events.
- `GET /bookings/{id}/invoice` returns invoice details for a paid booking. The endpoint currently returns JSON, not a rendered PDF.

Never mark an invoice paid based on client-side state. Payment status is driven by verified Stripe webhooks.

## Real-time chat

The local Vite server proxies `/ws` to the API with WebSocket upgrades enabled. If chat does not connect, confirm the API is running on port 8080, the frontend is opened from `localhost:5173` or `127.0.0.1:5173`, and the account has an active booking with that supplier.

Authenticated customers and suppliers can subscribe to `/user/queue/notifications` for real-time updates. Booking requests, offers, booking status changes, incoming chat messages, and collaboration invitations/acceptances/service additions/activation/selections create persistent in-app notifications for the affected user; the notification center shows unread counts and remains available after reconnecting. Notification responses contain either a `bookingId` or a `collaborationId`, depending on the event.

WhatsApp delivery is optional and disabled by default. To send each notification to the recipient's saved phone number after the notification transaction commits, set `WHATSAPP_ENABLED=true`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM`, and `TWILIO_WHATSAPP_CONTENT_SID` on the API. The content SID must refer to an approved Twilio WhatsApp template containing `{{1}} {{2}} {{3}}. Open Wedora to view the details.`; the variables are the actor, notification action, and event context. Users may save an E.164 number during registration or update it with `PUT /api/users/me/phone-number`.

Connect STOMP to `/ws`, send the JWT in the STOMP `CONNECT` native `Authorization: Bearer <JWT>` header, and subscribe to `/topic/bookings/{bookingId}`. Send messages to `/app/bookings/{bookingId}` with `{ "body": "…", "attachmentId": 123 }`. Upload a private attachment to `POST /bookings/{id}/attachments` as multipart `file`, then include its returned `id`; download links are participant-protected. JPEG, PNG, WebP, MP4, and PDF files are accepted up to 50 MB. Both chat history (`GET /bookings/{id}/messages`) and WebSocket subscriptions/sends verify that the signed-in account is a participant in that booking.

## Administration

All `/admin/**` routes require an administrator JWT:

- `GET/POST/PUT/DELETE /admin/categories[/{id}]` lists, adds, edits, and archives categories.
- `GET /admin/suppliers/pending` lists supplier applications.
- `PATCH /admin/suppliers/{id}/approval?approved=true|false` changes approval state.
- `GET /admin/analytics` returns basic user, supplier, approved supplier, and booking counts.

## Core entities

`AppUser`, `Category`, `Supplier`, `Booking`, `Payment`, `Message`, `ChecklistItem`, `Review`, and `Media` are persisted using JPA. The initial PostgreSQL schema lives in `backend/src/main/resources/db/migration/V1__initial_schema.sql`.
