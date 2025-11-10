package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderWebsocketSessionRegistryTest {

    @Test
    @DisplayName("GivenNewRegistry_WhenRegisterAndUnregisterStaffSession_ThenStaffSessionsUpdated")
    void GivenNewRegistry_WhenRegisterAndUnregisterStaffSession_ThenStaffSessionsUpdated() {
        //given
        var registry = new OrderWebsocketSessionRegistry();
        WebSocketSession session = mock(WebSocketSession.class);

        //when
        registry.registerStaffSession(session);

        //then
        assertThat(registry.getStaffSessions()).contains(session);

        //when
        registry.unregisterStaffSession(session);

        //then
        assertThat(registry.getStaffSessions()).doesNotContain(session);
    }

    @Test
    @DisplayName("GivenNoCustomerSessions_WhenGetCustomerSessions_ThenReturnEmptySet")
    void GivenNoCustomerSessions_WhenGetCustomerSessions_ThenReturnEmptySet() {
        //given
        var registry = new OrderWebsocketSessionRegistry();
        var orderId = OrderId.generate();

        //when
        Set<WebSocketSession> sessions = registry.getCustomerSessions(orderId);

        //then
        assertThat(sessions).isEmpty();
    }

    @Test
    @DisplayName("GivenCustomerSessions_WhenRegisterMultiple_ThenReturnSessions")
    void GivenCustomerSessions_WhenRegisterMultiple_ThenReturnSessions() {
        //given
        var registry = new OrderWebsocketSessionRegistry();
        var orderId = OrderId.generate();
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);

        //when
        registry.registerCustomerSession(orderId, s1);
        registry.registerCustomerSession(orderId, s2);

        //then
        assertThat(registry.getCustomerSessions(orderId)).containsExactlyInAnyOrder(s1, s2);
    }

    @Test
    @DisplayName("GivenCustomerSessions_WhenUnregisterLast_ThenRemoveMapping")
    void GivenCustomerSessions_WhenUnregisterLast_ThenRemoveMapping() {
        //given
        var registry = new OrderWebsocketSessionRegistry();
        var orderId = OrderId.generate();
        WebSocketSession s1 = mock(WebSocketSession.class);

        registry.registerCustomerSession(orderId, s1);
        assertThat(registry.getCustomerSessions(orderId)).contains(s1);

        //when
        registry.unregisterCustomerSession(orderId, s1);

        //then
        assertThat(registry.getCustomerSessions(orderId)).isEmpty();
    }

    @Test
    @DisplayName("GivenNoMapping_WhenUnregisterCustomerSession_ThenNoExceptionAndStillEmpty")
    void GivenNoMapping_WhenUnregisterCustomerSession_ThenNoExceptionAndStillEmpty() {
        //given
        var registry = new OrderWebsocketSessionRegistry();
        var orderId = OrderId.generate();
        WebSocketSession s1 = mock(WebSocketSession.class);

        //when (should not throw)
        registry.unregisterCustomerSession(orderId, s1);

        //then
        assertThat(registry.getCustomerSessions(orderId)).isEmpty();
    }

}

