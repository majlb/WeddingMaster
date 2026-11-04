package com.wedora;

import com.wedora.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WedoraApplicationTest {
    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationStartsAndSeedsWeddingCategories() {
        assertThat(categories.findBySlugAndActiveTrue("venues")).isPresent();
        assertThat(categories.findBySlugAndActiveTrue("photography")).isPresent();
        assertThat(categories.findBySlugAndActiveTrue("flowers").orElseThrow().getTranslations())
                .containsEntry("fr", java.util.Map.of(
                        "name", "Fleurs",
                        "description", "Fleurs pour votre mariage"));
    }

    @Test
    void localViteOriginCanReachPublicLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"diagnostic@example.invalid","password":"NotARealPassword-123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
