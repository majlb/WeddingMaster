package com.wedora.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MobileOriginTest {
    @Test
    void productionCorsAllowsAndroidOriginButNotArbitraryOrigins() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var source = new SecurityConfig().corsConfigurationSource("https://www.wedmasterai.com", environment);
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/categories"));

        assertEquals("https://localhost", cors.checkOrigin("https://localhost"));
        assertEquals("https://www.wedmasterai.com", cors.checkOrigin("https://www.wedmasterai.com"));
        assertNull(cors.checkOrigin("https://example.com"));
    }
}
