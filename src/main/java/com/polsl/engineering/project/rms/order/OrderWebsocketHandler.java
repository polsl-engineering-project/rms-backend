package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
class OrderWebsocketHandler extends TextWebSocketHandler {

    private final OrderService orderService;
    private final ObjectMapper om;
    private final OrderWebsocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sendInitialData(session);
        sessionRegistry.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregisterSession(session);
    }

    private void sendInitialData(WebSocketSession session) throws IOException {
        try {
            var message = new OrderPayloads.OrderWebsocketMessage(
                    "INITIAL_DATA",
                    orderService.getActiveOrders()
            );
            var payload = om.writeValueAsString(message);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Error sending initial data to staff websocket", e);
            session.close();
        }
    }

}
