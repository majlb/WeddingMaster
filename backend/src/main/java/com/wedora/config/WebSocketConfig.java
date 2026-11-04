package com.wedora.config;

import com.wedora.security.JwtService;
import com.wedora.service.BookingService;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwtService;
    private final BookingService bookingService;
    private final String frontendUrl;
    private final Environment environment;

    public WebSocketConfig(JwtService jwtService, BookingService bookingService,
                           @Value("${app.frontend-url}") String frontendUrl, Environment environment) {
        this.jwtService = jwtService;
        this.bookingService = bookingService;
        this.frontendUrl = frontendUrl;
        this.environment = environment;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> allowedOrigins = new java.util.ArrayList<>(List.of(frontendUrl));
        if (environment.matchesProfiles("!prod")) {
            allowedOrigins.add("http://localhost:5173");
            allowedOrigins.add("http://127.0.0.1:5173");
        }
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new AccessDeniedException("A bearer token is required for chat");
                    }
                    try {
                        var claims = jwtService.parse(authorization.substring(7));
                        String role = claims.get("role", String.class);
                        accessor.setUser(new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                    } catch (JwtException | IllegalArgumentException exception) {
                        throw new AccessDeniedException("Invalid or expired bearer token", exception);
                    }
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if ("/user/queue/notifications".equals(destination)) {
                        if (accessor.getUser() == null) {
                            throw new AccessDeniedException("Authentication is required");
                        }
                        return message;
                    }
                    String prefix = "/topic/bookings/";
                    if (destination == null || !destination.startsWith(prefix)) {
                        throw new AccessDeniedException("Subscriptions are limited to booking conversations");
                    }
                    try {
                        Long bookingId = Long.valueOf(destination.substring(prefix.length()));
                        if (accessor.getUser() == null) throw new AccessDeniedException("Authentication is required");
                        bookingService.requireParticipant(bookingId, accessor.getUser().getName());
                    } catch (NumberFormatException exception) {
                        throw new AccessDeniedException("Invalid booking destination", exception);
                    }
                }
                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    String prefix = "/app/bookings/";
                    if (destination == null || !destination.startsWith(prefix) || accessor.getUser() == null) {
                        throw new AccessDeniedException("Messages must be sent to a booking conversation");
                    }
                    try {
                        Long bookingId = Long.valueOf(destination.substring(prefix.length()));
                        bookingService.requireParticipant(bookingId, accessor.getUser().getName());
                    } catch (NumberFormatException exception) {
                        throw new AccessDeniedException("Invalid booking destination", exception);
                    }
                }
                return message;
            }
        });
    }
}
