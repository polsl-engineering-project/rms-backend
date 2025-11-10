package com.polsl.engineering.project.rms.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@EnableWebSocket
@Configuration
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketConfigurer {

    private final List<ModuleWebsocketConfig> moduleWebsocketConfigs;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        moduleWebsocketConfigs.forEach(x -> x.registerWebSocketHandlers(registry));
    }

    public interface ModuleWebsocketConfig {
        void registerWebSocketHandlers(WebSocketHandlerRegistry registry);
    }

}
