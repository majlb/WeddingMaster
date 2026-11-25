package com.wedora.config;

import com.wedora.security.JwtService;
import com.wedora.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileWebSocketOriginTest {
    @Test
    void productionWebSocketAllowsAndroidAndConfiguredWebOrigin() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var registry = mock(StompEndpointRegistry.class);
        var registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        var config = new WebSocketConfig(mock(JwtService.class), mock(BookingService.class),
                "https://www.wedmasterai.com", environment);

        config.registerStompEndpoints(registry);

        verify(registration).setAllowedOrigins("https://www.wedmasterai.com", "https://localhost");
    }
}
