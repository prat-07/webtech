# BorrowBuddy

BorrowBuddy is a campus lending marketplace. The frontend is React, TypeScript and Vite; the API is Spring Boot with PostgreSQL, Spring Data JPA, Flyway and server-side sessions.

## Run locally

Requirements: Java 21, Maven, Node.js 20.19+, npm and Docker Compose.

Start the database in one terminal:

```sh
docker compose up -d db
```

Start the API in another:

```sh
cd backend
mvn spring-boot:run
```

Start the frontend in a third:

```sh
cd frontend
npm ci
npm run dev
```

Open the URL Vite prints (normally `http://localhost:5173`). Vite proxies `/api` requests to Spring Boot on port 8081. PostgreSQL is published on `127.0.0.1:5433` so a local PostgreSQL service on port 5432 can remain running. Keep the API and database running while using the web app; if the API is stopped, the UI will report that it cannot connect.

Flyway initializes/updates the schema at API startup. The dev profile uses local-only database defaults; production requires `DATABASE_URL`, `DATABASE_USER`, and `DATABASE_PASSWORD`, disables public OpenAPI docs, and expects HTTPS. Set `COOKIE_SECURE=true` when the browser connects over HTTPS.

## Authentication and security

Every app visit checks the server session. Signed-out users are redirected to `/login`; they can switch to `/signup`. The backend requires authentication for listings and all borrower/lender actions. Authentication uses a server-side session, HttpOnly session cookie, CSRF tokens for mutations, and BCrypt password hashing. The API derives the acting user from the authenticated principal, scopes listings to the user's campus, and applies owner/borrower checks to loan actions.

## Current features

- Search campus listings and filter by category
- View listing details and lender reputation
- Save and unsave listings
- Publish a listing with an optional HTTPS image URL
- Request an item for a selected borrow and return date
- View and cancel your borrow requests
- Review requests for your listings, approve/decline, confirm handover and confirm return
- View notifications and mark them read
- Persist listings, loans, saved items, sessions and notifications in PostgreSQL

In development, Swagger UI is available at `http://localhost:8081/swagger-ui.html` to authenticated users. Reviews have an API endpoint but are not yet exposed in the interface. Email verification, password recovery, image uploads, abuse throttling, deployment infrastructure and automated end-to-end/a11y coverage remain follow-up work before a public production launch.
