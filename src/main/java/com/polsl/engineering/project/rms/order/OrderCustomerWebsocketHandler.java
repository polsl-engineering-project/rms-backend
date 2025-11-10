package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
class OrderCustomerWebsocketHandler extends TextWebSocketHandler {

    private final OrderWebsocketSessionRegistry sessionRegistry;
    private final ObjectMapper om;
    private final OrderRepository orderRepository;

    private static final String ERROR_MESSAGE_SERIALIZATION_ERROR = "Error creating error message JSON";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var getOrderIdResult = getOrderIdFromSession(session);
        if (getOrderIdResult.isFailure()) {
            try {
                var errorMsg = getErrorMessage(getOrderIdResult.getError());
                session.sendMessage(errorMsg);
            } catch (JsonProcessingException e) {
                log.error(ERROR_MESSAGE_SERIALIZATION_ERROR, e);
            }
            session.close();
            return;
        }

        var orderId = getOrderIdResult.getValue();
        var optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            try {
                var errorMsg = getErrorMessage("Order not found for given orderId");
                session.sendMessage(errorMsg);
            } catch (JsonProcessingException e) {
                log.error(ERROR_MESSAGE_SERIALIZATION_ERROR, e);
            }
            session.close();
            return;
        }

        if (optionalOrder.get().isFinished()) {
            try {
                var errorMsg = getErrorMessage("Order is finished");
                session.sendMessage(errorMsg);
            } catch (JsonProcessingException e) {
                log.error(ERROR_MESSAGE_SERIALIZATION_ERROR, e);
            }
            session.close();
            return;
        }

        sessionRegistry.registerCustomerSession(orderId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var getOrderIdResult = getOrderIdFromSession(session);
        if (getOrderIdResult.isFailure()) {
            return;
        }

        var orderId = getOrderIdResult.getValue();
        sessionRegistry.unregisterCustomerSession(orderId, session);
    }

    private Result<OrderId> getOrderIdFromSession(WebSocketSession session) {
        var rawOrderId = session.getAttributes().get("orderId");
        if (rawOrderId == null) {
            return Result.failure("Missing orderId in session attributes");
        }

        OrderId orderId;
        try {
            orderId = OrderId.from(rawOrderId.toString());
        } catch (InvalidUUIDFormatException _) {
            return Result.failure("Invalid orderId format");
        }

        return Result.ok(orderId);
    }

    private TextMessage getErrorMessage(String errorMessage) throws JsonProcessingException {
        var msgObj = OrderPayloads.OrderWebsocketMessage.error(errorMessage);
        var msgJson = om.writeValueAsString(msgObj);
        return new TextMessage(msgJson);
    }

}
