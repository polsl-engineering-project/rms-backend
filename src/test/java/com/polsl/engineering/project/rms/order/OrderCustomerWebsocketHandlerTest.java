package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import com.polsl.engineering.project.rms.order.vo.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCustomerWebsocketHandlerTest {

    @Mock
    OrderWebsocketSessionRegistry sessionRegistry;

    @Mock
    OrderRepository orderRepository;

    ObjectMapper om;

    OrderCustomerWebsocketHandler handler;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();
        handler = new OrderCustomerWebsocketHandler(sessionRegistry, om, orderRepository);
    }

    @Test
    @DisplayName("GivenMissingOrderId_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession")
    void GivenMissingOrderId_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession() throws Exception {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());

        //when
        handler.afterConnectionEstablished(session);

        //then
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("Missing orderId in session attributes");
        verify(session).close();
    }

    @Test
    @DisplayName("GivenInvalidOrderIdFormat_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession")
    void GivenInvalidOrderIdFormat_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession() throws Exception {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = Map.of("orderId", "not-a-uuid");
        when(session.getAttributes()).thenReturn(attrs);

        //when
        handler.afterConnectionEstablished(session);

        //then
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("Invalid orderId format");
        verify(session).close();
    }

    @Test
    @DisplayName("GivenNonExistingOrder_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession")
    void GivenNonExistingOrder_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession() throws Exception {
        //given
        var orderId = OrderId.generate();
        String idStr = orderId.value().toString();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("orderId", idStr));
        when(orderRepository.findById(OrderId.from(idStr))).thenReturn(Optional.empty());

        //when
        handler.afterConnectionEstablished(session);

        //then
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("Order not found for given orderId");
        verify(session).close();
    }

    @Test
    @DisplayName("GivenFinishedOrder_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession")
    void GivenFinishedOrder_WhenAfterConnectionEstablished_ThenSendErrorAndCloseSession() throws Exception {
        //given
        var orderId = OrderId.generate();
        String idStr = orderId.value().toString();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("orderId", idStr));

        var finishedOrder = Order.reconstruct(
                OrderId.from(idStr),
                OrderType.PICKUP,
                DeliveryMode.ASAP,
                OrderStatus.COMPLETED,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                0L
        );

        when(orderRepository.findById(OrderId.from(idStr))).thenReturn(Optional.of(finishedOrder));

        //when
        handler.afterConnectionEstablished(session);

        //then
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("Order is finished");
        verify(session).close();
    }

    @Test
    @DisplayName("GivenActiveOrder_WhenAfterConnectionEstablished_ThenRegisterSession")
    void GivenActiveOrder_WhenAfterConnectionEstablished_ThenRegisterSession() throws Exception {
        //given
        var orderId = OrderId.generate();
        String idStr = orderId.value().toString();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("orderId", idStr));

        var activeOrder = Order.reconstruct(
                OrderId.from(idStr),
                OrderType.PICKUP,
                DeliveryMode.ASAP,
                OrderStatus.CONFIRMED,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                0L
        );

        when(orderRepository.findById(OrderId.from(idStr))).thenReturn(Optional.of(activeOrder));

        //when
        handler.afterConnectionEstablished(session);

        //then
        verify(sessionRegistry).registerCustomerSession(OrderId.from(idStr), session);
        verify(session, never()).sendMessage(any());
        verify(session, never()).close();
    }

    @Test
    @DisplayName("GivenMissingOrderId_WhenAfterConnectionClosed_ThenDoNothing")
    void GivenMissingOrderId_WhenAfterConnectionClosed_ThenDoNothing() {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());

        //when
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        //then
        verifyNoInteractions(sessionRegistry);
    }

    @Test
    @DisplayName("GivenValidOrderId_WhenAfterConnectionClosed_ThenUnregisterSession")
    void GivenValidOrderId_WhenAfterConnectionClosed_ThenUnregisterSession() {
        //given
        var orderId = OrderId.generate();
        String idStr = orderId.value().toString();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("orderId", idStr));

        //when
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        //then
        verify(sessionRegistry).unregisterCustomerSession(OrderId.from(idStr), session);
    }

}