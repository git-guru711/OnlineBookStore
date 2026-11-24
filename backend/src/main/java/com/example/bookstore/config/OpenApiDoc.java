package com.example.bookstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All API documentation. springdoc generates paths and schemas from the controllers; this class adds summaries,
 * responses, examples and public endpoints. A renamed endpoint makes generation fail, so the docs cannot go stale.
 */
@Configuration
public class OpenApiDoc {

    static final String BASIC_AUTH = "basicAuth";
    static final String PROBLEM_SCHEMA = "Problem";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    private static final String BOOKS = "Books";
    private static final String ACCOUNT = "Account";
    private static final String CART = "Cart";
    private static final String ORDERS = "Orders";

    private static final String NOT_LOGGED_IN = "Not logged in (`unauthorized`)";
    private static final String STOCK_CONFLICT = "Not enough stock (`insufficient-stock`) or a concurrent change "
            + "(`concurrent-modification`, safe to retry)";

    @Bean
    OpenAPI bookstoreOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Online Bookstore API").version("v1").description("""
                                REST API of the Online Bookstore: catalog, accounts, cart and orders.

                                **Authentication:** HTTP Basic (`Authorization: Basic base64(email:password)`), stateless.
                                The catalog and registration are public; everything else needs a user. In Swagger UI,
                                click **Authorize** and log in, e.g. with the demo account `demo@example.com` / `demo1234`
                                """))
                .components(new Components()
                        .addSecuritySchemes(
                                BASIC_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Email and password of a registered user")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH)) // default: login needed
                .tags(List.of(
                        new Tag().name(ACCOUNT).description("Registration and the logged-in user"),
                        new Tag()
                                .name(BOOKS)
                                .description("The catalog with prices and stock. Public: no login needed."),
                        new Tag()
                                .name(CART)
                                .description("The logged-in user's own cart. Adding to the cart never "
                                        + "reserves or reduces stock."),
                        new Tag().name(ORDERS).description("Checkout and the logged-in user's order history")));
    }

    // The Problem schema is added here: springdoc drops unreferenced schemas before customizers run.
    @Bean
    OpenApiCustomizer bookstoreEndpointDocs() {
        return api -> {
            api.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
            documentBooks(api);
            documentAccount(api);
            documentCart(api);
            documentOrders(api);
            documentRequestExamples(api);
        };
    }

    private static void documentBooks(OpenAPI api) {
        endpoint(api, HttpMethod.GET, "/api/v1/books")
                .tag(BOOKS)
                .publicAccess()
                .summary("List or search books")
                .description("One page of the catalog. With `q`, only books whose title or author contains the text "
                        + "(ignoring case, matched literally). No match returns an empty page.")
                .param("page", "Page number, starting at 0", 0)
                .param("size", "Books per page (1-100)", 20)
                .param(
                        "sort",
                        "`field,direction` with field `title`, `author` or `price` and direction `asc` or `desc`",
                        "price,desc")
                .param("q", "Search text for title or author (at most 100 characters)", "martin")
                .ok("200", "A page of books (possibly empty)")
                .error("400", "Invalid page, size, sort or q (`validation-error` / `malformed-request`)");

        endpoint(api, HttpMethod.GET, "/api/v1/books/{id}")
                .tag(BOOKS)
                .publicAccess()
                .summary("Get one book")
                .description("A single book with its price and current stock.")
                .param("id", "Book id", 1)
                .ok("200", "The book")
                .error("400", "The id is not a number (`malformed-request`)")
                .error("404", "No book with this id (`not-found`)");
    }

    private static void documentAccount(OpenAPI api) {
        endpoint(api, HttpMethod.POST, "/api/v1/auth/register")
                .tag(ACCOUNT)
                .publicAccess()
                .summary("Register a new account")
                .description("Creates a user with role USER. The email is trimmed and lower-cased. "
                        + "Afterwards, call the other endpoints with HTTP Basic (email and password).")
                .ok("201", "Account created; `Location: /api/v1/auth/user`")
                .error("400", "Invalid fields (`validation-error`) or unreadable JSON (`malformed-request`)")
                .error("409", "The email is already registered (`email-already-registered`)");

        endpoint(api, HttpMethod.GET, "/api/v1/auth/user")
                .tag(ACCOUNT)
                .summary("Get the logged-in user (login check)")
                .description("200 means the Basic credentials are valid; the frontend uses it as \"login\". "
                        + "A 401 carries no `WWW-Authenticate` header, so browsers show no login popup.")
                .ok("200", "The logged-in user")
                .error("401", "Missing or wrong credentials (`unauthorized`)");
    }

    private static void documentCart(OpenAPI api) {
        endpoint(api, HttpMethod.GET, "/api/v1/cart")
                .tag(CART)
                .summary("Get my cart")
                .description("Lines with current prices and stock, the subtotal and whether checkout is allowed. "
                        + "A user without a cart gets an empty one, never 404.")
                .ok("200", "The cart")
                .error("401", NOT_LOGGED_IN);

        endpoint(api, HttpMethod.POST, "/api/v1/cart/items")
                .tag(CART)
                .summary("Add a book to my cart")
                .description("If the book is already in the cart, the quantities are added up.")
                .ok("201", "New line created; returns the whole cart")
                .ok("200", "Merged into an existing line; returns the whole cart")
                .error("400", "Quantity not 1-99 or invalid body (`validation-error` / `malformed-request`)")
                .error("401", NOT_LOGGED_IN)
                .error("404", "No book with this id (`not-found`)")
                .error("409", STOCK_CONFLICT);

        endpoint(api, HttpMethod.PATCH, "/api/v1/cart/items/{bookId}")
                .tag(CART)
                .summary("Change the quantity of a cart line")
                .description("Sets the quantity (1-99) of one line.")
                .param("bookId", "Id of the book in the cart", 1)
                .ok("200", "The updated cart")
                .error("400", "Quantity not 1-99 or invalid body (`validation-error` / `malformed-request`)")
                .error("401", NOT_LOGGED_IN)
                .error("404", "This book is not in the cart (`not-found`)")
                .error("409", STOCK_CONFLICT);

        endpoint(api, HttpMethod.DELETE, "/api/v1/cart/items/{bookId}")
                .tag(CART)
                .summary("Remove a line from my cart")
                .param("bookId", "Id of the book in the cart", 1)
                .noContent("Removed")
                .error("401", NOT_LOGGED_IN)
                .error("404", "This book is not in the cart (`not-found`)");

        endpoint(api, HttpMethod.DELETE, "/api/v1/cart")
                .tag(CART)
                .summary("Empty my cart")
                .description("Idempotent: emptying an empty cart also answers 204.")
                .noContent("The cart is empty")
                .error("401", NOT_LOGGED_IN);
    }

    private static void documentOrders(OpenAPI api) {
        endpoint(api, HttpMethod.POST, "/api/v1/orders")
                .tag(ORDERS)
                .summary("Check out (place an order from my cart)")
                .description("No body: the order is built from the cart on the server. In one transaction the stock is "
                        + "reduced, the order is saved with the current prices and the cart is emptied. Send an "
                        + "`Idempotency-Key` so that a retry cannot order twice.")
                .param(
                        "Idempotency-Key",
                        "Optional, recommended: one key per purchase attempt (e.g. a UUID), reused "
                                + "for every retry. 8-64 letters, digits or hyphens.",
                        "3f2b8c4e-1a7d-4e9b-9c2a-5d6e7f8a9b0c")
                .ok("201", "Order placed; `Location: /api/v1/orders/{id}`")
                .ok(
                        "200",
                        "Repeat of an earlier request with the same Idempotency-Key: the original order, nothing changed")
                .error("400", "The cart is empty (`cart-empty`) or the key is malformed (`validation-error`)")
                .error("401", NOT_LOGGED_IN)
                .error(
                        "409",
                        "Not enough stock (`insufficient-stock`, nothing changed) or a concurrent request "
                                + "(`concurrent-modification`, safe to retry with the same key)");

        endpoint(api, HttpMethod.GET, "/api/v1/orders")
                .tag(ORDERS)
                .summary("List my orders")
                .description("Order history, newest first, one page at a time.")
                .param("page", "Page number, starting at 0", 0)
                .param("size", "Orders per page (1-100)", 20)
                .ok("200", "A page of order summaries (possibly empty)")
                .error("400", "Invalid page or size (`validation-error` / `malformed-request`)")
                .error("401", NOT_LOGGED_IN);

        endpoint(api, HttpMethod.GET, "/api/v1/orders/{id}")
                .tag(ORDERS)
                .summary("Get one of my orders")
                .description("With its lines and the prices paid.")
                .param("id", "Order id", 1)
                .ok("200", "The order")
                .error("401", NOT_LOGGED_IN)
                .error("404", "No such order, or it belongs to another user (`not-found`)");
    }

    /** Readable examples; otherwise Swagger UI fills the password with random text from the validation regex. */
    private static void documentRequestExamples(OpenAPI api) {
        property(api, "RegisterRequest", "email").setExample("jane@example.com");
        property(api, "RegisterRequest", "password")
                .example("secret123")
                .description("8-72 characters, at least one letter and one digit");
        property(api, "RegisterRequest", "fullName").setExample("Jane Doe");
        property(api, "AddCartItemRequest", "bookId").setExample(1);
        property(api, "AddCartItemRequest", "quantity").setExample(1);
        property(api, "UpdateCartItemRequest", "quantity").setExample(2);
    }

    /** The RFC 7807 body written by GlobalExceptionHandler. */
    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .description("RFC 7807 problem details")
                .addProperty(
                        "type",
                        new StringSchema()
                                .format("uri")
                                .example("https://bookstore.example/problems/insufficient-stock")
                                .description("Stable error type; clients branch on this"))
                .addProperty("title", new StringSchema().example("Insufficient stock"))
                .addProperty("status", new IntegerSchema().example(409))
                .addProperty("detail", new StringSchema().example("Not enough stock for one or more books"))
                .addProperty("instance", new StringSchema().example("/api/v1/orders"))
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty(
                        "errors",
                        new ArraySchema()
                                .items(new ObjectSchema())
                                .description("Details: failing fields for validation-error ({field, message}), "
                                        + "books for insufficient-stock ({bookId, title, requested, available})"));
    }

    private static Schema<?> property(OpenAPI api, String schemaName, String propertyName) {
        Schema<?> schema = api.getComponents().getSchemas().get(schemaName);
        if (schema == null
                || schema.getProperties() == null
                || !schema.getProperties().containsKey(propertyName)) {
            throw new IllegalStateException("API docs: no property " + schemaName + "." + propertyName);
        }
        return schema.getProperties().get(propertyName);
    }

    private static Endpoint endpoint(OpenAPI api, HttpMethod method, String path) {
        PathItem pathItem = api.getPaths().get(path);
        Operation operation =
                pathItem == null ? null : pathItem.readOperationsMap().get(method);
        if (operation == null) {
            throw new IllegalStateException("API docs: no endpoint " + method + " " + path);
        }
        return new Endpoint(operation);
    }

    /** Replaces springdoc's responses: successes keep the generated body schema, errors get the Problem schema. */
    private static final class Endpoint {

        private final Operation operation;
        private final Content successBody;

        Endpoint(Operation operation) {
            this.operation = operation;
            ApiResponses generated = operation.getResponses();
            this.successBody = generated == null
                    ? null
                    : generated.entrySet().stream()
                            .filter(e -> e.getKey().startsWith("2"))
                            .map(Map.Entry::getValue)
                            .map(ApiResponse::getContent)
                            .filter(content -> content != null && !content.isEmpty())
                            .findFirst()
                            .orElse(null);
            operation.setResponses(new ApiResponses());
        }

        Endpoint tag(String tag) {
            operation.setTags(List.of(tag));
            return this;
        }

        Endpoint summary(String summary) {
            operation.setSummary(summary);
            return this;
        }

        Endpoint description(String description) {
            operation.setDescription(description);
            return this;
        }

        /** No login needed: an empty security list overrides the global HTTP Basic requirement. */
        Endpoint publicAccess() {
            operation.setSecurity(new ArrayList<>());
            return this;
        }

        Endpoint param(String name, String description, Object example) {
            Parameter parameter = operation.getParameters() == null
                    ? null
                    : operation.getParameters().stream()
                            .filter(p -> name.equals(p.getName()))
                            .findFirst()
                            .orElse(null);
            if (parameter == null) {
                throw new IllegalStateException("API docs: no parameter " + name + " on " + operation.getOperationId());
            }
            parameter.description(description).example(example);
            return this;
        }

        Endpoint ok(String code, String description) {
            operation
                    .getResponses()
                    .addApiResponse(
                            code, new ApiResponse().description(description).content(successBody));
            return this;
        }

        Endpoint noContent(String description) {
            operation.getResponses().addApiResponse("204", new ApiResponse().description(description));
            return this;
        }

        Endpoint error(String code, String description) {
            operation
                    .getResponses()
                    .addApiResponse(
                            code,
                            new ApiResponse()
                                    .description(description)
                                    .content(new Content()
                                            .addMediaType(
                                                    PROBLEM_MEDIA_TYPE,
                                                    new MediaType()
                                                            .schema(new Schema<>()
                                                                    .$ref("#/components/schemas/" + PROBLEM_SCHEMA)))));
            return this;
        }
    }
}
