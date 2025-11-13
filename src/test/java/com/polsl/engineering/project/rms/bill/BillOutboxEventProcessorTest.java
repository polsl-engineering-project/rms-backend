package com.polsl.engineering.project.rms.bill;

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
class BillOutboxEventProcessorTest {

    @Captor
    ArgumentCaptor<TextMessage> messageCaptor;

    @Test
    @DisplayName("Given valid outbox event, When processEvent, Then send websocket message and mark event processed")
    void GivenValidEvent_WhenProcessEvent_ThenSendsMessageAndSetsProcessedTrue() throws Exception {
        // given
        var registry = mock(BillWebsocketSessionRegistry.class);
        var session = mock(WebSocketSession.class);
        when(registry.getSessions()).thenReturn(Set.of(session));

        var om = new ObjectMapper();
        var processor = new BillOutboxEventProcessor(registry, om);

        var payload = "{\"billId\":\"test-123\"}";
        var event = BillOutboxEvent.of(com.polsl.engineering.project.rms.bill.vo.BillId.generate(),
                com.polsl.engineering.project.rms.bill.event.BillEventType.OPENED,
                payload);

        // when
        processor.processEvent(event);

        // then
        verify(session, times(1)).sendMessage(messageCaptor.capture());
        var sent = messageCaptor.getValue();
        assertThat(sent).isNotNull();
        assertThat(sent.getPayload()).contains("OPENED");
        assertThat(sent.getPayload()).contains("\"billId\":\"test-123\"");
        assertThat(event.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Given invalid JSON payload, When processEvent, Then do not mark event processed and do not send messages")
    void GivenInvalidJson_WhenProcessEvent_ThenDoesNotSetProcessed() throws Exception {
        // given
        var registry = mock(BillWebsocketSessionRegistry.class);
        var om = mock(ObjectMapper.class);

        // make readTree throw JsonProcessingException
        when(om.readTree(anyString())).thenThrow(new JsonProcessingException("invalid"){});

        var processor = new BillOutboxEventProcessor(registry, om);

        var payload = "not-a-json";
        var event = BillOutboxEvent.of(com.polsl.engineering.project.rms.bill.vo.BillId.generate(),
                com.polsl.engineering.project.rms.bill.event.BillEventType.LINES_ADDED,
                payload);

        // when
        processor.processEvent(event);

        // then
        assertThat(event.isProcessed()).isFalse();
        // ensure no attempts to send messages when parsing fails
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("Given multiple sessions, When processEvent, Then send message to all sessions")
    void GivenMultipleSessions_WhenProcessEvent_ThenSendsToAllSessions() throws Exception {
        // given
        var registry = mock(BillWebsocketSessionRegistry.class);
        var session1 = mock(WebSocketSession.class);
        var session2 = mock(WebSocketSession.class);
        var session3 = mock(WebSocketSession.class);
        when(registry.getSessions()).thenReturn(Set.of(session1, session2, session3));

        var om = new ObjectMapper();
        var processor = new BillOutboxEventProcessor(registry, om);

        var payload = "{\"data\":\"test\"}";
        var event = BillOutboxEvent.of(com.polsl.engineering.project.rms.bill.vo.BillId.generate(),
                com.polsl.engineering.project.rms.bill.event.BillEventType.CLOSED,
                payload);

        // when
        processor.processEvent(event);

        // then
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));
        assertThat(event.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Given session throws exception, When processEvent, Then continue processing and mark event processed")
    void GivenSessionThrows_WhenProcessEvent_ThenContinuesAndMarksProcessed() throws Exception {
        // given
        var registry = mock(BillWebsocketSessionRegistry.class);
        var session1 = mock(WebSocketSession.class);
        var session2 = mock(WebSocketSession.class);

        doThrow(new RuntimeException("connection failed")).when(session1).sendMessage(any(TextMessage.class));

        when(registry.getSessions()).thenReturn(Set.of(session1, session2));

        var om = new ObjectMapper();
        var processor = new BillOutboxEventProcessor(registry, om);

        var payload = "{\"test\":\"data\"}";
        var event = BillOutboxEvent.of(com.polsl.engineering.project.rms.bill.vo.BillId.generate(),
                com.polsl.engineering.project.rms.bill.event.BillEventType.LINES_REMOVED,
                payload);

        // when
        processor.processEvent(event);

        // then
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        assertThat(event.isProcessed()).isTrue();
    }
}