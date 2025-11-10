package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.common.config.WebsocketConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
class OrderWebsocketConfig implements WebsocketConfig.ModuleWebsocketConfig {

    private final OrderStaffWebsocketHandler staffWebsocketHandler;
    private final OrderCustomerWebsocketHandler customerWebsocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(staffWebsocketHandler, "/ws/orders/staff")
                .setAllowedOrigins("*");
        registry.addHandler(customerWebsocketHandler, "/ws/order-status")
                .addInterceptors(new OrderCustomerWebsocketInterceptor())
                .setAllowedOrigins("*");
    }

}
