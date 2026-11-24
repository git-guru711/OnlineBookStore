# Bookstore backend

The REST API of the Online Bookstore (`/api/v1`): book catalog with stock, search, accounts, carts and orders. The React app in [`../frontend`](../frontend) uses it. To run the whole project, see the [main README](../README.md).

**Stack:** Java 21 · Spring Boot 4.2 · Spring Security (HTTP Basic) · Spring Data JPA / Hibernate · H2 · springdoc (Swagger UI) · JUnit 6 + Mockito + MockMvc

---

## Contents

1. [Commands](#1-commands)
2. [Dependencies and versions](#2-dependencies-and-versions)
3. [Folder structure](#3-folder-structure)
4. [How a request flows](#4-how-a-request-flows)
5. [Best practices followed](#5-best-practices-followed)

---

## 1. Commands

Run inside `backend/`. Maven does not need to be installed: `./mvnw` (the Maven wrapper) downloads Maven 3.9.9 on first use.

| Command | What it does |
|---|---|
| `./mvnw spring-boot:run` | Starts the API on http://localhost:8080 with the `dev` profile (H2 console at `/h2-console`, SQL logging); see [Dev JVM options](#dev-jvm-options) to start it with the dev JVM options |
| `./mvnw test` | Runs all 239 tests (unit, slice and integration) and writes the coverage report to `target/site/jacoco/index.html` |
| `./mvnw verify` | Tests, then the checks: fails if less than 80% of lines or branches are covered, or if any file is not formatted |
| `./mvnw spotless:apply` | Formats all Java code (Palantir Java Format) and trims trailing whitespace in YAML and SQL files |
| `./mvnw clean package` | Tests, then builds `target/bookstore-backend-0.0.1-SNAPSHOT.jar` |
| `java -jar target/bookstore-backend-0.0.1-SNAPSHOT.jar` | Runs the built jar (started from `backend/`, it also picks up `config/dev/application.yml`) |

### Dev JVM options

Pass the JVM options when starting the backend from the command line:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-XX:+UseG1GC -Xms1024m -Xmx1024m -Dspring.config.location=config/dev/ -Dspring.config.name=application"
```

**From IntelliJ:** use the shared run configuration **DEV** in [`.run/DEV.run.xml`](.run/DEV.run.xml). When `backend/` is opened as the project, IntelliJ picks it up automatically: choose **DEV** in the run menu and click Run. It starts `BookstoreApplication` with these VM options:

```
-XX:+UseG1GC -Xms1024m -Xmx1024m -Dspring.config.location=config/dev/ -Dspring.config.name=application
```

`config/dev/` is relative to the working directory, which is `backend/` when that is the IntelliJ project.

| Option | Effect |
|---|---|
| `-XX:+UseG1GC` | G1 garbage collector (short, predictable pauses) |
| `-Xms1024m -Xmx1024m` | Heap fixed at 1 GB: starts at its full size and never grows or shrinks |
| `-Dspring.config.location=config/dev/` | Read configuration **only** from `config/dev/` (relative to `backend/`); `src/main/resources/application*.yml` are not read |
| `-Dspring.config.name=application` | The file there is called `application` (`application.yml`) |

With these options, Spring reads only `config/dev/application.yml`, so it holds the complete configuration (database, JPA, port, CORS, springdoc), sets `spring.profiles.active: dev`, and has a `dev` section with the H2 console and SQL logging. `schema.sql` and `data.sql` still come from `src/main/resources`.

Without the options (plain `./mvnw spring-boot:run`, or `java -jar` from `backend/`), Spring reads `src/main/resources/application.yml` and then also finds `config/dev/application.yml` on its own (it always looks in `./config/*/`), so the dev profile is on either way.

Tests never read `config/dev/`: Surefire limits them to the classpath (`src/main/resources` and `src/test/resources/config/`). When you change a setting, change it in both `config/dev/application.yml` and `src/main/resources/application.yml`.

With the backend running:

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI: browse and try every endpoint (**Authorize** with `demo@example.com` / `demo1234`) |
| http://localhost:8080/v3/api-docs | OpenAPI 3 description (JSON), e.g. for Postman |
| http://localhost:8080/h2-console | Database console, `dev` profile only (JDBC URL `jdbc:h2:mem:bookstore`, user `sa`, no password) |

The database is in memory: it starts with 30 books and the demo account on every start, and resets when the backend stops.

---

## 2. Dependencies and versions

Spring Boot's parent POM (`spring-boot-starter-parent` 4.2.0) manages the versions of everything it knows, so `pom.xml` names a version only for springdoc. The versions below are what the build actually resolves (`./mvnw dependency:list`).

### Platform

| Tool | Version | Why |
|---|---|---|
| Java | 21 (LTS) | Records, pattern matching, text blocks; long-term support |
| Spring Boot | 4.2.0 | Auto-configuration, embedded server, dependency management |
| Maven (via wrapper) | 3.9.9 | Same build tool version on every machine |

### Runtime dependencies (`pom.xml`)

| Dependency | Version | Brings in | Used for |
|---|---|---|---|
| `spring-boot-starter-webmvc` | 4.2.0 | Spring MVC 7.1.0, Tomcat 11.0.27, Jackson 3.1.8 | REST controllers, JSON, embedded web server |
| `spring-boot-starter-validation` | 4.2.0 | Hibernate Validator 9.1.6 | `@Valid`, `@NotBlank`, `@Size`, `@Min`... on requests and parameters |
| `spring-boot-starter-data-jpa` | 4.2.0 | Spring Data JPA 4.2.0, Hibernate 7.4.15, HikariCP 7.1.0 | Entities, repositories, transactions, connection pool |
| `spring-boot-starter-security` | 4.2.0 | Spring Security 7.2.0 | HTTP Basic login, BCrypt password hashing, access rules, CORS |
| `h2` (runtime) | 2.5.252 | – | In-memory database in MySQL compatibility mode, zero setup |
| `springdoc-openapi-starter-webmvc-ui` | 3.2.1 | Swagger UI 5.35.1 | OpenAPI description and Swagger UI generated from the controllers |
| `spring-boot-h2console` (runtime) | 4.2.0 | – | The H2 web console (switched on only in the `dev` profile) |

### Test dependencies

| Dependency | Version | Brings in | Used for |
|---|---|---|---|
| `spring-boot-starter-webmvc-test` | 4.2.0 | JUnit Jupiter 6.1.3, Mockito 5.24.0, AssertJ 3.27.7, MockMvc | Unit tests, controller slice tests (`@WebMvcTest`), integration tests |
| `spring-boot-starter-data-jpa-test` | 4.2.0 | – | Repository slice tests (`@DataJpaTest`) against H2 |
| `spring-boot-starter-security-test` | 4.2.0 | – | Logged-in users in tests (`with(user(...))` on MockMvc requests) |

### Plugins

| Plugin | Used for |
|---|---|
| `spring-boot-maven-plugin` | `spring-boot:run` and the runnable jar |
| `maven-surefire-plugin` | Runs `*Test`, `*Tests` and `*IT` classes in `./mvnw test` |
| `jacoco-maven-plugin` 0.8.16 | Test coverage: report on `test`, 80% line and branch check on `verify` |
| `spotless-maven-plugin` 3.11.0 | Formatting with Palantir Java Format 2.103.0 (4-space indent, 120 columns) and unused-import removal; `spotless:check` runs on `verify` |

---

## 3. Folder structure

Package by layer: each package has one job, and calls go one way (controller → service → repository).

```
backend/
├── pom.xml                       dependencies and build
├── config/dev/application.yml    complete dev settings, with the dev profile active
├── .run/DEV.run.xml              IntelliJ run configuration "DEV": BookstoreApplication with the dev JVM options
├── mvnw, mvnw.cmd, .mvn/         Maven wrapper (no local Maven needed)
└── src/
    ├── main/
    │   ├── java/com/example/bookstore/
    │   │   ├── BookstoreApplication.java   entry point (@SpringBootApplication)
    │   │   ├── controller/      REST endpoints: HTTP in, JSON out. No business rules
    │   │   ├── service/         business rules and transactions
    │   │   ├── repository/      database access (Spring Data JPA interfaces, JPQL queries)
    │   │   ├── entity/          JPA entities = tables; domain rules live on them (e.g. Cart.addItem)
    │   │   ├── model/           API shapes: request/response records, enums, SortParameter
    │   │   ├── exception/       domain exceptions + GlobalExceptionHandler (errors → RFC 7807)
    │   │   └── config/          security, CORS, password encoder, API docs, the logged-in user
    │   └── resources/
    │       ├── application.yml       settings: H2, JPA, port 8080, CORS origins, springdoc
    │       ├── application-dev.yml   dev profile: H2 console, SQL logging
    │       ├── schema.sql            tables and constraints (the schema is owned by SQL, not Hibernate)
    │       └── data.sql              30 demo books with stock, and the demo account
    └── test/java/com/example/bookstore/   same packages as main, plus support/ (shared test helpers)
```

### What is in each package

| Package | Classes |
|---|---|
| `controller` | `BookController` (`/books`), `AuthController` (`/auth`), `CartController` (`/cart`), `OrderController` (`/orders`) |
| `service` | `BookService`, `AuthService`, `CartService`, `InventoryService` (stock), `OrderService` (checkout), `IdempotentCheckout` (retry-safe checkout), `BookstoreUserDetailsService` (loads the user for login) |
| `repository` | `BookRepository`, `InventoryRepository`, `UserRepository`, `CartRepository`, `OrderRepository` |
| `entity` | `Book`, `Inventory`, `User`, `Cart`, `CartItem`, `Order`, `OrderItem` |
| `model` | `*Request` (input), `*Response` (output), `PageResponse<T>`, `BookView`, enums (`Role`, `StockStatus`, `OrderStatus`), `SortParameter` |
| `exception` | `ResourceNotFoundException` (404), `InsufficientStockException` / `EmailAlreadyRegisteredException` / `ConcurrentRequestException` (409), `EmptyCartException` / `InvalidQuantityException` / `InvalidRequestParameterException` (400), `GlobalExceptionHandler` |
| `config` | `SecurityConfig`, `WebConfig` + `CorsProperties`, `PasswordConfig` (BCrypt), `OpenApiDoc` (Swagger), `AuthenticatedUser` (the logged-in principal) |

### Entities and their relationships

Seven entities, one table each (defined in `schema.sql`):

| Entity (table) | Holds | Key fields |
|---|---|---|
| `Book` (`books`) | A book in the catalog | `isbn` (unique), `title`, `author`, `price` |
| `Inventory` (`inventory`) | How many copies of a book are in stock | `book_id`, `available_quantity` (never below 0), `version` |
| `User` (`users`) | A customer account | `email` (unique), `password_hash` (BCrypt), `full_name`, `role` |
| `Cart` (`carts`) | A user's shopping cart | `user_id` (unique: one cart per user), `version` |
| `CartItem` (`cart_items`) | One book in a cart | `cart_id`, `book_id`, `quantity` (1–99; a book appears once per cart) |
| `Order` (`orders`) | A placed order | `user_id`, `status`, `total_amount`, `created_at`, `idempotency_key` |
| `OrderItem` (`order_items`) | One book in an order | `order_id`, `book_id`, `title`, `unit_price`, `quantity` |

```
Book  1 ── 1  Inventory        every book has one stock row

User  1 ── 1  Cart  1 ── *  CartItem  * ── 1  Book
                                               (the cart points to live books: current price and stock)

User  1 ── *  Order  1 ── *  OrderItem ·····  Book
                                               (only the book id; title and price are copied at checkout)
```

- **Cart vs order:** a cart is live and changeable, and always shows today's prices. An order is a snapshot: its lines copy the title and price, so it never changes when the catalog does. That's also why `order_items` has no foreign key to `books`.
- **Stock lives apart from the book**, so the frequent stock updates at checkout touch only the small `inventory` row.
- **`version` columns** (inventory, cart) enable optimistic locking: if two requests change the same row at once, one gets 409 instead of silently overwriting the other.
### Tests

| Kind | Example | What it checks |
|---|---|---|
| Unit | `CartTest`, `OrderServiceTest` | A class alone, collaborators mocked |
| Repository slice | `BookRepositoryTest` | Queries against a real H2 database |
| Controller slice | `CartControllerTest` | HTTP mapping, validation, status codes, JSON shape (service mocked) |
| Integration (`*IT`) | `CheckoutFlowIT`, `SecurityConfigIT` | The whole app: security, services, database |
| Concurrency | `CheckoutContentionIT`, `CheckoutConcurrencyIT` | Parallel checkouts: no overselling, no deadlocks, one order per idempotency key |

---

## 4. How a request flows

Example: a logged-in user adds a book to the cart.

```
POST /api/v1/cart/items  {"bookId": 1, "quantity": 2}   Authorization: Basic ZGVtb0BleGFtcGxlLmNvbTpkZW1vMTIzNA==
   │
   ▼
Security filter chain (SecurityConfig)
   │  CORS check → HTTP Basic: BookstoreUserDetailsService loads the user, BCrypt checks the password
   │  → AuthenticatedUser (id, email, role) in the security context. Wrong or missing → 401 problem
   ▼
CartController.addItem(@AuthenticationPrincipal user, @Valid AddCartItemRequest)
   │  validation fails → 400 validation-error (field list)
   ▼
CartService.addItem(userId, bookId, quantity)          @Transactional
   │  BookRepository: the book exists?   InventoryService: enough stock?
   │  CartRepository: the user's cart (created on first use)
   │  Cart.addItem(...): merge with an existing line or add a new one (domain rule on the entity)
   ▼
Commit → CartResponse (lines, current prices and stock, subtotal, checkoutAllowed)
   │
   ▼
201 Created (new line) or 200 OK (merged), JSON body
```

Any exception on the way (book missing, not enough stock, concurrent change) reaches `GlobalExceptionHandler`, which answers with the matching status and an RFC 7807 problem (`application/problem+json`) with a stable `type`, e.g. `.../problems/insufficient-stock`.

### Checkout (`POST /api/v1/orders`)

```
OrderController ─► IdempotentCheckout ─► OrderService.checkout (one transaction)
                     │                     1. load the cart; empty → 400 cart-empty
                     │                     2. InventoryService: lock rows in book-id order, then
                     │                        UPDATE inventory SET qty = qty - n WHERE qty >= n
                     │                        any line short → 409 insufficient-stock, nothing changes
                     │                     3. create the order with copied prices, empty the cart
                     │
                     └─ same Idempotency-Key again → the original order (200), no second purchase
```

---

## 5. Best practices followed

**Testing**
- **Test-driven development**: every behaviour started as a failing test, then the code to make it pass; tests that passed at once were checked by breaking the code on purpose.
- **Consistent formatting**: Spotless formats the code with Palantir Java Format and fails `./mvnw verify` on unformatted files, so style never comes up in reviews. Run `./mvnw spotless:apply` before committing.
- **Coverage gate**: JaCoCo fails `./mvnw verify` below 80% of lines or branches. Currently 99% of lines and 95% of branches are covered.
- **Outside-in**: each increment began with an integration test of the whole use case, then unit and slice tests for the pieces.
- **The right test for each layer**: plain unit tests for rules, `@DataJpaTest` for queries, `@WebMvcTest` for HTTP, `@SpringBootTest` for the whole flow, and real parallel threads for concurrency.

**API design**
- Versioned REST API (`/api/v1`), plural nouns, correct status codes (201 + `Location`, 204, 400, 401, 404, 409).
- **One error format**: RFC 7807 problem details everywhere, including 401 and unknown URLs, with a stable `type` clients can branch on.
- **Validation at the edge**: Bean Validation on request bodies and parameters; sort fields come from an allow-list.
- **Paging** for every list (`page`, `size` up to 100, `sort`) returned as one `PageResponse` shape.
- **Documented contract**: Swagger UI and OpenAPI generated from the code, so the docs cannot drift; all documentation text lives in `OpenApiDoc`, keeping controllers and models clean.

**Architecture**
- **Layered**: thin controllers, business rules in services and entities, data access in repositories.
- **DTOs, not entities, in the API**: request/response records decouple the JSON from the database and never leak password hashes.
- **Constructor injection** everywhere (no field injection), so classes are easy to test.
- **Configuration outside the code**: port, database and CORS origins in `application.yml`, overridable by environment variables (`CORS_ALLOWED_ORIGINS`); dev-only features in a profile.
- **Schema owned by SQL** (`schema.sql`), with Hibernate set to `validate`: startup fails if the entities and the tables disagree.

**Data integrity and concurrency**
- **Prices come from the server**: checkout builds the order from the stored cart and current prices; the client never sends prices.
- **No overselling**: stock is decreased with a conditional `UPDATE ... WHERE available >= n` in the checkout transaction; if any line is short, everything rolls back.
- **No deadlocks**: stock rows are always locked in book-id order.
- **Optimistic locking** (`@Version`) on carts and inventory; conflicts answer 409 `concurrent-modification` (safe to retry) instead of 500.
- **Idempotent checkout**: an `Idempotency-Key` header plus a `UNIQUE (user_id, idempotency_key)` constraint guarantees one order per purchase attempt, even for double clicks arriving at the same moment.
- **Database constraints** back up the code: foreign keys, unique email, non-negative stock, quantities 1–99.

**Security**
- **Deny by default**: only the catalog, registration and the API docs are public; everything else needs a login.
- **Stateless**: no server sessions or cookies, so no CSRF risk; every request carries its credentials.
- **BCrypt** password hashes; passwords limited to 72 characters (BCrypt's input limit) so none is silently truncated.
- **Users only see their own data**: the user comes from the login, never from the URL; another user's order answers 404, not 403, so ids reveal nothing.
- **No browser login popup**: 401 answers carry no `WWW-Authenticate: Basic` header.
- **CORS allow-list** for the frontend's origin; H2 console only in the `dev` profile; error details only in logs, never in responses.
