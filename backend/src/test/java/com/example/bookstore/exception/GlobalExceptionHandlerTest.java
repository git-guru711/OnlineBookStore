package com.example.bookstore.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.model.StockShortage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private static final String TYPE_BASE = "https://bookstore.example/problems/";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void unexpectedException_returns500ProblemWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "internal-error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value("/test/boom"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(content().string(not(containsString("secret internal state"))));
    }

    @Test
    void resourceNotFound_returns404Problem() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Book 99 not found"));
    }

    @Test
    void emailAlreadyRegistered_returns409Problem() throws Exception {
        mockMvc.perform(get("/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "email-already-registered"))
                .andExpect(jsonPath("$.title").value("Email already registered"))
                .andExpect(jsonPath("$.detail").value("Email jane@example.com is already registered"));
    }

    @Test
    void insufficientStock_returns409WithShortageLines() throws Exception {
        mockMvc.perform(get("/test/out-of-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "insufficient-stock"))
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.errors[0].bookId").value(4))
                .andExpect(jsonPath("$.errors[0].title").value("Domain-Driven Design"))
                .andExpect(jsonPath("$.errors[0].requested").value(2))
                .andExpect(jsonPath("$.errors[0].available").value(1));
    }

    @Test
    void invalidQuantity_returns400ValidationErrorOnQuantity() throws Exception {
        mockMvc.perform(get("/test/bad-quantity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Quantity must be between 1 and 99 (this would make 120)"));
    }

    @Test
    void optimisticLockFailure_returns409ConcurrentModification() throws Exception {
        mockMvc.perform(get("/test/concurrent-edit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "concurrent-modification"))
                .andExpect(jsonPath("$.detail")
                        .value("The data was changed by another request. Please reload and try again."));
    }

    @Test
    void emptyCart_returns400CartEmpty() throws Exception {
        mockMvc.perform(get("/test/empty-cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "cart-empty"))
                .andExpect(jsonPath("$.title").value("Cart is empty"))
                .andExpect(jsonPath("$.detail").value("Your cart is empty"));
    }

    @Test
    void concurrentRequest_returns409ConcurrentModification() throws Exception {
        mockMvc.perform(get("/test/concurrent-create"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "concurrent-modification"));
    }

    @Test
    void uniqueConstraintRace_returns409ConcurrentModification_notA500() throws Exception {
        // e.g. two checkouts with the same Idempotency-Key
        mockMvc.perform(get("/test/duplicate-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "concurrent-modification"))
                .andExpect(jsonPath("$.detail")
                        .value("The data was changed by another request. Please reload and try again."));
    }

    @Test
    void databaseLockFailure_returns409ConcurrentModification() throws Exception {
        mockMvc.perform(get("/test/lock-timeout"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "concurrent-modification"))
                .andExpect(jsonPath("$.detail")
                        .value("The data was changed by another request. Please reload and try again."));
    }

    @Test
    void invalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "validation-error"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("bookId"))
                .andExpect(jsonPath("$.errors[1].field").value("quantity"))
                .andExpect(jsonPath("$.errors[1].message", notNullValue()));
    }

    @Test
    void unreadableJson_returns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/test/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "malformed-request"));
    }

    @Test
    void pathVariableTypeMismatch_returns400MalformedRequest() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "malformed-request"));
    }

    @Test
    void wrongHttpMethod_returns405Problem() throws Exception {
        mockMvc.perform(post("/test/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "method-not-allowed"));
    }

    @Test
    void unsupportedContentType_returns415Problem() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value(TYPE_BASE + "unsupported-media-type"));
    }

    @Test
    void frameworkProblem_thatAlreadyHasItsOwnType_keepsIt() {
        ProblemDetail ownType = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        ownType.setType(URI.create("https://other.example/problems/custom"));

        ResponseEntity<Object> response = new GlobalExceptionHandler()
                .handleExceptionInternal(
                        new IllegalStateException("x"),
                        ownType,
                        new HttpHeaders(),
                        HttpStatus.METHOD_NOT_ALLOWED,
                        webRequest());

        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body.getType()).hasToString("https://other.example/problems/custom");
        assertThat(body.getProperties()).isNull(); // left alone: no timestamp added
    }

    @Test
    void frameworkError_withABodyThatIsNotAProblem_isPassedOnUnchanged() {
        ResponseEntity<Object> response = new GlobalExceptionHandler()
                .handleExceptionInternal(
                        new IllegalStateException("x"),
                        "plain text",
                        new HttpHeaders(),
                        HttpStatus.BAD_REQUEST,
                        webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("plain text");
    }

    @Test
    void frameworkError_afterTheResponseWasAlreadySent_writesNothing() {
        MockHttpServletResponse committed = new MockHttpServletResponse();
        committed.setCommitted(true);

        ResponseEntity<Object> response = new GlobalExceptionHandler()
                .handleExceptionInternal(
                        new IllegalStateException("x"),
                        null,
                        new HttpHeaders(),
                        HttpStatus.METHOD_NOT_ALLOWED,
                        new ServletWebRequest(new MockHttpServletRequest("POST", "/test/boom"), committed));

        assertThat(response).isNull();
    }

    private static ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest("POST", "/test/boom"), new MockHttpServletResponse());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal state");
        }

        @GetMapping("/test/missing")
        String missing() {
            throw new ResourceNotFoundException("Book 99 not found");
        }

        @GetMapping("/test/duplicate-email")
        String duplicateEmail() {
            throw new EmailAlreadyRegisteredException("jane@example.com");
        }

        @GetMapping("/test/out-of-stock")
        String outOfStock() {
            throw new InsufficientStockException(List.of(new StockShortage(4L, "Domain-Driven Design", 2, 1)));
        }

        @GetMapping("/test/bad-quantity")
        String badQuantity() {
            throw new InvalidQuantityException("Quantity must be between 1 and 99 (this would make 120)");
        }

        @GetMapping("/test/duplicate-key")
        String duplicateKey() {
            throw new org.springframework.dao.DataIntegrityViolationException("Unique index or primary key violation");
        }

        @GetMapping("/test/concurrent-edit")
        String concurrentEdit() {
            throw new ObjectOptimisticLockingFailureException("Cart", 1L);
        }

        @GetMapping("/test/empty-cart")
        String emptyCart() {
            throw new EmptyCartException();
        }

        @GetMapping("/test/concurrent-create")
        String concurrentCreate() {
            throw new ConcurrentRequestException();
        }

        @GetMapping("/test/lock-timeout")
        String lockTimeout() {
            throw new CannotAcquireLockException("Deadlock detected or lock wait timeout");
        }

        @GetMapping("/test/items/{id}")
        String item(@PathVariable Long id) {
            return "ok";
        }

        @PostMapping("/test/items")
        String create(@Valid @RequestBody TestItemRequest request) {
            return "ok";
        }
    }

    record TestItemRequest(
            @NotNull Long bookId, @NotNull @Min(1) @Max(99) Integer quantity) {}
}
