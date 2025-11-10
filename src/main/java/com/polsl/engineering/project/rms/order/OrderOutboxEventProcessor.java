package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Slf4j
@Component
@RequiredArgsConstructor
class OrderOutboxEventProcessor {

    private final OrderWebsocketSessionRegistry websocketSessionRegistry;
    private final ObjectMapper om;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void processEvent(OrderOutboxEvent event) {
        try {
            var deserialize = om.readTree(event.getPayload());
            var msgObj = new OrderPayloads.OrderWebsocketMessage(
                    event.getType().name(),
                    deserialize
            );

            var msgJson = om.writeValueAsString(msgObj);
            var msgWebSocket = new TextMessage(msgJson);

            websocketSessionRegistry.getSessions().forEach(session -> {
                try {
                    session.sendMessage(msgWebSocket);
                } catch (Exception e) {
                    log.error("Failed to send websocket message to session {}", session.getId(), e);
                }
            });

            event.setProcessed(true);
        } catch (JsonProcessingException e) {
            log.error("Failed to process outbox event {}", event.getId(), e);
        }
    }

}
