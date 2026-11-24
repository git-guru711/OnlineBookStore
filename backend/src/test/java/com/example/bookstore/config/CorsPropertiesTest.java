package com.example.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void missingSetting_meansNoOriginIsAllowed() {
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }

    @Test
    void origins_areCopied_soLaterChangesToTheSourceListDoNotLeakIn() {
        List<String> source = new ArrayList<>(List.of("http://localhost:5173"));
        CorsProperties properties = new CorsProperties(source);

        source.add("https://evil.example");

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:5173");
        assertThatThrownBy(() -> properties.allowedOrigins().add("https://evil.example"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
