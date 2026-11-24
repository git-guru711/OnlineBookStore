package com.example.bookstore.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The demo catalog has 30 books, so the default page size of 20 gives two pages. */
@SpringBootTest
@AutoConfigureMockMvc
class BooksApiIT {

    private static final int DEMO_CATALOG_SIZE = 30;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listBooks_returnsSeededBooksWithPriceAndStockStatus() throws Exception {
        // size=100: all books on one page
        mockMvc.perform(get("/api/v1/books").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(DEMO_CATALOG_SIZE))
                .andExpect(
                        jsonPath("$.content[?(@.title == 'Clean Code')].author").value(hasItem("Robert C. Martin")))
                .andExpect(
                        jsonPath("$.content[?(@.title == 'Clean Code')].price").value(hasItem(32.99)))
                .andExpect(jsonPath("$.content[?(@.title == 'Clean Code')].availableQuantity")
                        .value(hasItem(15)))
                .andExpect(jsonPath("$.content[?(@.title == 'Clean Code')].stockStatus")
                        .value(hasItem("IN_STOCK")))
                .andExpect(jsonPath("$.content[?(@.title == 'Refactoring')].availableQuantity")
                        .value(hasItem(0)))
                .andExpect(jsonPath("$.content[?(@.title == 'Refactoring')].stockStatus")
                        .value(hasItem("OUT_OF_STOCK")));
    }

    @Test
    void listBooks_splitsTheCatalogIntoPagesOfTwenty() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(DEMO_CATALOG_SIZE))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(20));

        mockMvc.perform(get("/api/v1/books").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content.length()").value(DEMO_CATALOG_SIZE - 20));
    }

    @Test
    void search_byTitle_findsMatchingBooksIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("q", "CLEAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].title")
                        .value(contains("Clean Agile", "Clean Architecture", "Clean Code", "The Clean Coder")));
    }

    @Test
    void search_byAuthor_findsTheirBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("q", "vernon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title")
                        .value(contains("Domain-Driven Design Distilled", "Implementing Domain-Driven Design")));
    }

    @Test
    void search_withNoMatches_returnsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("q", "no such book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void search_withEmptyQ_returnsTheWholeCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(DEMO_CATALOG_SIZE));
    }
}
