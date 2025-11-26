package com.polsl.engineering.project.rms.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.bill.event.BillEventType;
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
class BillWebsocketHandler extends TextWebSocketHandler {

    private final BillService billService;
    private final ObjectMapper om;
    private final BillWebsocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
        if (authenticated == null || !authenticated) {
            log.warn("Unauthenticated Bill WebSocket connection attempt");
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        sendInitialData(session);
        sessionRegistry.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregisterSession(session);
    }

    private void sendInitialData(WebSocketSession session) throws IOException {
        try {
            var message = new BillPayloads.BillWebsocketMessage(
                    BillEventType.INITIAL_DATA.toString(),
                    billService.getOpenBills()
            );
            var payload = om.writeValueAsString(message);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Error sending initial data to staff websocket", e);
            session.close();
        }
    }

}
