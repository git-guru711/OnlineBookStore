# Online Bookstore

An online bookstore where customers browse books, add them to a cart and check out. It consists of two applications that talk over a REST API.

## Projects

| | Backend | Frontend |
|---|---|---|
| Folder | [`backend/`](backend) | [`frontend/`](frontend) |
| Purpose | REST API (`/api/v1`): catalog, stock, accounts, carts, orders | Web app in the browser |
| Technology | Java 21, Spring Boot 4.2, Spring Security, Spring Data JPA, H2 | React 19, TypeScript, Vite, Material UI, TanStack Query, React Router |
| Tests | JUnit 5, Mockito, MockMvc | Vitest, React Testing Library, MSW |
| Runs on | http://localhost:8080 | http://localhost:5173 |

Each application has its own README with its dependencies and versions, folder structure, flow and best practices: [backend/README.md](backend/README.md) · [frontend/README.md](frontend/README.md).

**Features:** book catalog with covers, prices and stock status · search by title or author · registration and login · cart (add, change quantity, remove) · checkout with order summary · order confirmation and order history.

## Prerequisites

- Java 21
- Node.js 20 or later (with npm)

Maven is not needed: the backend includes the Maven wrapper (`./mvnw`).

## Build

Backend:

```bash
cd backend
./mvnw clean package
```

Creates `backend/target/bookstore-backend-0.0.1-SNAPSHOT.jar` (runs the tests first; add `-DskipTests` to skip them).

Frontend:

```bash
cd frontend
npm install
VITE_API_BASE_URL=/api/v1 npm run build
```

Creates the production files in `frontend/dist/`.

## Run tests

Backend:

```bash
cd backend
./mvnw test      # all tests; coverage report in target/site/jacoco/index.html
./mvnw verify    # tests + coverage check (80% of lines and branches) + format check
./mvnw spotless:apply   # format the code
```

Frontend:

```bash
cd frontend
npm run test:run
npm run lint
npm run format:check   # Prettier; npm run format fixes it
npm run coverage   # tests + coverage check: fails below 80%; report in coverage/index.html
```

## Configure

| Setting | Where | Default | Purpose |
|---|---|---|---|
| `server.port` and all backend settings | `backend/config/dev/application.yml` | `8080` | Backend port, database, CORS, dev profile |
| JVM options | passed when starting the backend (see below) | `-XX:+UseG1GC -Xms1024m -Xmx1024m` | Garbage collector and heap size |
| `CORS_ALLOWED_ORIGINS` | environment variable for the backend | `http://localhost:5173` | Origins allowed to call the API |
| `VITE_API_BASE_URL` | `frontend/.env.development` | `/api/v1` | Where the frontend sends API calls |
| `BACKEND_URL` | `frontend/.env.development` | `http://localhost:8080` | Where the frontend dev server forwards `/api` |

For local overrides, create `frontend/.env.development.local` (not committed), e.g. `BACKEND_URL=http://localhost:9090`.

The backend uses an in-memory H2 database with 30 demo books and a demo account; the data resets on every restart.

## Run the application

Start the backend first, then the frontend, each in its own terminal.

**Development:**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-XX:+UseG1GC -Xms1024m -Xmx1024m -Dspring.config.location=config/dev/ -Dspring.config.name=application"
```

(Plain `./mvnw spring-boot:run` works too, with the JVM's default memory settings.)

**From IntelliJ:** open `backend/` as the project and pick the ready-made run configuration **DEV** (stored in `backend/.run/DEV.run.xml`, picked up automatically) from the run menu, then click Run. It starts `BookstoreApplication` with the same JVM options.

```bash
cd frontend
npm install
npm run dev
```

**Built version** (after [Build](#build)):

```bash
cd backend
java -jar target/bookstore-backend-0.0.1-SNAPSHOT.jar
```

```bash
cd frontend
npm run preview
```

Open **http://localhost:5173** and log in with the demo account `demo@example.com` / `demo1234`, or register a new one. Stop each application with Ctrl+C.

## API documentation (Swagger)

With the backend running, open **http://localhost:8080/swagger-ui.html** to browse and try every endpoint. Click **Authorize** and log in with `demo@example.com` / `demo1234` for the endpoints that need a user (cart, orders). The OpenAPI description (JSON) is at http://localhost:8080/v3/api-docs, e.g. for Postman or client generators.

## Query the database (H2 console)

The `dev` profile adds the H2 web console and logs every SQL statement. It is on whenever the backend starts as described above: `backend/config/dev/application.yml` sets `spring.profiles.active: dev`.

Open **http://localhost:8080/h2-console** and connect with:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:bookstore` (replace the default `jdbc:h2:~/test`) |
| User Name | `sa` |
| Password | *(empty)* |

Then type SQL and click **Run**, e.g. `SELECT * FROM books;`. Without the `dev` profile the console does not exist (`/h2-console` answers 401).
