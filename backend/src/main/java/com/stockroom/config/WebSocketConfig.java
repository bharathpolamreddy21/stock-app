package com.stockroom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket / STOMP configuration.
 *
 * Clients connect to:  /ws  (with SockJS fallback for environments that
 *                           don't support native WebSocket — e.g. some proxies)
 *
 * Subscribe to:  /topic/stock-updates  — broadcast channel for stock changes
 *
 * K8s note:
 *   WebSocket connections are long-lived. The Ingress must forward Upgrade
 *   and Connection headers. In the ingress manifest we add:
 *     nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
 *     nginx.ingress.kubernetes.io/configuration-snippet: |
 *       proxy_set_header Upgrade $http_upgrade;
 *       proxy_set_header Connection "Upgrade";
 *
 *   With multiple replicas, connections go to different pods. For a real
 *   production app you'd add Redis pub/sub so all pods share the broker.
 *   For this training exercise, sticky sessions on the Ingress are enough:
 *     nginx.ingress.kubernetes.io/affinity: "cookie"
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for /topic destinations
        registry.enableSimpleBroker("/topic");
        // Prefix for messages routed to @MessageMapping controller methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();   // SockJS fallback for older browsers / proxies
    }
}
