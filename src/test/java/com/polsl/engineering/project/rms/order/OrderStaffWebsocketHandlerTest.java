package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStaffWebsocketHandlerTest {

    @Mock
    OrderService orderService;

    @Mock
    OrderWebsocketSessionRegistry sessionRegistry;

    OrderStaffWebsocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderStaffWebsocketHandler(orderService, new ObjectMapper(), sessionRegistry);
    }

    @Test
    @DisplayName("GivenActiveOrders_WhenAfterConnectionEstablished_ThenSendInitialDataAndRegisterSession")
    void GivenActiveOrders_WhenAfterConnectionEstablished_ThenSendInitialDataAndRegisterSession() throws Exception {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        when(orderService.getActiveOrders()).thenReturn(List.of());
        var realOm = new ObjectMapper();
        handler = new OrderStaffWebsocketHandler(orderService, realOm, sessionRegistry);

        //when
        handler.afterConnectionEstablished(session);

        //then
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("INITIAL_DATA");
        verify(sessionRegistry).registerStaffSession(session);
    }

    @Test
    @DisplayName("GivenOrderServiceThrows_WhenAfterConnectionEstablished_ThenCloseSessionAndRegisterSession")
    void GivenOrderServiceThrows_WhenAfterConnectionEstablished_ThenCloseSessionAndRegisterSession() throws Exception {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        when(orderService.getActiveOrders()).thenThrow(new RuntimeException("boom"));
        var realOm = new ObjectMapper();
        handler = new OrderStaffWebsocketHandler(orderService, realOm, sessionRegistry);

        //when
        handler.afterConnectionEstablished(session);

        //then
        verify(session, never()).sendMessage(any());
        verify(session).close();
        verify(sessionRegistry).registerStaffSession(session);
    }

    @Test
    @DisplayName("GivenSession_WhenAfterConnectionClosed_ThenUnregisterStaffSession")
    void GivenSession_WhenAfterConnectionClosed_ThenUnregisterStaffSession() {
        //given
        WebSocketSession session = mock(WebSocketSession.class);
        handler = new OrderStaffWebsocketHandler(orderService, new ObjectMapper(), sessionRegistry);

        //when
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        //then
        verify(sessionRegistry).unregisterStaffSession(session);
    }

}
