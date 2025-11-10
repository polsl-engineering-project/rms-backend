package com.polsl.engineering.project.rms.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCustomerWebsocketInterceptorTest {

    OrderCustomerWebsocketInterceptor interceptor;

    @Mock
    ServletServerHttpRequest servletServerHttpRequest;

    @Mock
    HttpServletRequest servletRequest;

    @Mock
    ServerHttpRequest otherRequest;

    @Mock
    ServerHttpResponse response;

    @Mock
    WebSocketHandler handler;

    @BeforeEach
    void setUp() {
        interceptor = new OrderCustomerWebsocketInterceptor();
    }

    @Test
    @DisplayName("GivenServletRequestWithOrderId_WhenBeforeHandshake_ThenPutOrderIdAttribute")
    void GivenServletRequestWithOrderId_WhenBeforeHandshake_ThenPutOrderIdAttribute() {
        //given
        when(servletServerHttpRequest.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getParameter("orderId")).thenReturn("0001-xyz");
        Map<String, Object> attributes = new HashMap<>();

        //when
        var result = interceptor.beforeHandshake(servletServerHttpRequest, response, handler, attributes);

        //then
        assertThat(result).isTrue();
        assertThat(attributes).containsEntry("orderId", "0001-xyz");
    }

    @Test
    @DisplayName("GivenServletRequestWithoutOrderId_WhenBeforeHandshake_ThenPutNullOrderIdAttribute")
    void GivenServletRequestWithoutOrderId_WhenBeforeHandshake_ThenPutNullOrderIdAttribute() {
        //given
        when(servletServerHttpRequest.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getParameter("orderId")).thenReturn(null);
        Map<String, Object> attributes = new HashMap<>();

        //when
        var result = interceptor.beforeHandshake(servletServerHttpRequest, response, handler, attributes);

        //then
        assertThat(result).isTrue();
        assertThat(attributes).containsKey("orderId");
        assertThat(attributes.get("orderId")).isNull();
    }

    @Test
    @DisplayName("GivenNonServletRequest_WhenBeforeHandshake_ThenDoNotModifyAttributes")
    void GivenNonServletRequest_WhenBeforeHandshake_ThenDoNotModifyAttributes() {
        //given
        Map<String, Object> attributes = new HashMap<>();
        ServerHttpRequest nonServlet = mock(ServerHttpRequest.class);

        //when
        var result = interceptor.beforeHandshake(nonServlet, response, handler, attributes);

        //then
        assertThat(result).isTrue();
        assertThat(attributes).isEmpty();
    }

}