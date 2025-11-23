package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.config.WebsocketConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
class OrderWebsocketConfig implements WebsocketConfig.ModuleWebsocketConfig {

    private final OrderWebsocketHandler staffWebsocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(staffWebsocketHandler, "/ws/orders")
                .setAllowedOrigins("*");
    }

}
