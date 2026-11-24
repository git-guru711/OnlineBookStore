package com.example.bookstore.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

/** A renamed endpoint or parameter must make doc generation fail instead of letting the docs go stale. */
class OpenApiDocTest {

    private final OpenApiDoc openApiDoc = new OpenApiDoc();

    @Test
    void anEndpointThatNoLongerExists_failsWithItsMethodAndPath() {
        OpenAPI withoutEndpoints = new OpenAPI().components(new Components()).paths(new Paths());

        assertThatThrownBy(() -> openApiDoc.bookstoreEndpointDocs().customise(withoutEndpoints))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("API docs: no endpoint GET /api/v1/books");
    }

    @Test
    void aParameterThatNoLongerExists_failsWithItsName() {
        // GET /api/v1/books exists, but without its parameters (and without generated responses)
        OpenAPI withoutParameters = new OpenAPI()
                .components(new Components())
                .paths(new Paths()
                        .addPathItem("/api/v1/books", new PathItem().get(new Operation().operationId("list"))));

        assertThatThrownBy(() -> openApiDoc.bookstoreEndpointDocs().customise(withoutParameters))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("API docs: no parameter page on list");
    }
}
