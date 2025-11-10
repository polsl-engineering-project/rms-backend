package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOutboxEventProcessorTest {

    @Captor
    ArgumentCaptor<TextMessage> messageCaptor;

    @Test
    @DisplayName("Given valid outbox event, When processEvent, Then send websocket message and mark event processed")
    void GivenValidEvent_WhenProcessEvent_ThenSendsMessageAndSetsProcessedTrue() throws Exception {
        // given
        var registry = mock(OrderWebsocketSessionRegistry.class);
        var session = mock(WebSocketSession.class);
        when(registry.getSessions()).thenReturn(Set.of(session));

        var om = new ObjectMapper();
        var processor = new OrderOutboxEventProcessor(registry, om);

        var payload = "{\"foo\":\"bar\"}";
        var event = OrderOutboxEvent.of(com.polsl.engineering.project.rms.order.vo.OrderId.generate(),
                com.polsl.engineering.project.rms.order.event.OrderEventType.DELIVERY_ORDER_PLACED,
                payload);

        // when
        processor.processEvent(event);

        // then
        verify(session, times(1)).sendMessage(messageCaptor.capture());
        var sent = messageCaptor.getValue();
        assertThat(sent).isNotNull();
        assertThat(sent.getPayload()).contains("DELIVERY_ORDER_PLACED");
        assertThat(sent.getPayload()).contains("\"foo\":\"bar\"");
        assertThat(event.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Given invalid JSON payload, When processEvent, Then do not mark event processed and do not send messages")
    void GivenInvalidJson_WhenProcessEvent_ThenDoesNotSetProcessed() throws Exception {
        // given
        var registry = mock(OrderWebsocketSessionRegistry.class);
        var om = mock(ObjectMapper.class);

        // make readTree throw JsonProcessingException
        when(om.readTree(anyString())).thenThrow(new JsonProcessingException("invalid"){});

        var processor = new OrderOutboxEventProcessor(registry, om);

        var payload = "not-a-json";
        var event = OrderOutboxEvent.of(com.polsl.engineering.project.rms.order.vo.OrderId.generate(),
                com.polsl.engineering.project.rms.order.event.OrderEventType.DELIVERY_ORDER_PLACED,
                payload);

        // when
        processor.processEvent(event);

        // then
        assertThat(event.isProcessed()).isFalse();
        // ensure no attempts to send messages when parsing fails
        verifyNoInteractions(registry);
    }

}