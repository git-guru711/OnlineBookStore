package com.example.bookstore.config;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theOpenApiDescriptionIsPublic_andListsEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(containsString("3.")))
                .andExpect(jsonPath("$.info.title").value("Online Bookstore API"))
                .andExpect(jsonPath("$.paths['/api/v1/books'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/books/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/user'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart/items'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/cart/items/{bookId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart/items/{bookId}'].delete")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{id}'].get").exists());
    }

    @Test
    void httpBasicIsDeclared_publicEndpointsNeedNoLogin_protectedOnesDo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(
                        jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme")
                        .value("basic"))
                .andExpect(jsonPath("$.security[0].basicAuth").exists()) // default: login needed
                .andExpect(jsonPath("$.paths['/api/v1/books'].get.security").value(empty())) // catalog: public
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.security")
                        .value(empty()))
                .andExpect(jsonPath("$.paths['/api/v1/cart'].get.security").doesNotExist());
    }

    @Test
    void everyEndpointHasATagAndASummary() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths.*.*").value(hasSize(12)))
                .andExpect(jsonPath("$.paths.*.*.tags[0]").value(hasSize(12)))
                .andExpect(jsonPath("$.paths.*.*.summary").value(hasSize(12)))
                .andExpect(jsonPath("$.tags[*].name").value(containsInAnyOrder("Account", "Books", "Cart", "Orders")));
    }

    @Test
    void endpointsAreDescribed_includingSearchAndTheIdempotencyKey() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/books'].get.summary").isString())
                .andExpect(jsonPath("$.paths['/api/v1/books'].get.parameters[*].name")
                        .value(hasItem("q")))
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.parameters[*].name")
                        .value(hasItem("Idempotency-Key")))
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['201']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['409']")
                        .exists())
                // the logged-in user comes from the Authorization header, not from a request parameter
                .andExpect(jsonPath("$.paths['/api/v1/cart'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.parameters[*].name")
                        .value(not(hasItem("user"))));
    }

    @Test
    void successesAreJson_errorsAreProblemDetails() throws Exception {
        String checkout = "$.paths['/api/v1/orders'].post.responses";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(checkout + "['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/OrderResponse"))
                .andExpect(jsonPath(checkout + "['409'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/Problem"))
                .andExpect(jsonPath(checkout + "['409'].content['application/json']")
                        .doesNotExist())
                .andExpect(jsonPath(checkout + "['201'].content['*/*']").doesNotExist())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/books/{id}'].get.responses['404'].content['application/problem+json']")
                        .exists())
                .andExpect(
                        jsonPath("$.components.schemas.Problem.properties.type").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.errors")
                        .exists());
    }

    /** Without examples, Swagger UI fills the register form with a random password from the validation regex. */
    @Test
    void requestBodiesHaveReadableExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.email.example")
                        .value("jane@example.com"))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.password.example")
                        .value("secret123"))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.fullName.example")
                        .value("Jane Doe"))
                .andExpect(jsonPath("$.components.schemas.AddCartItemRequest.properties.bookId.example")
                        .value(1))
                .andExpect(jsonPath("$.components.schemas.AddCartItemRequest.properties.quantity.example")
                        .value(1))
                .andExpect(jsonPath("$.components.schemas.UpdateCartItemRequest.properties.quantity.example")
                        .value(2));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
