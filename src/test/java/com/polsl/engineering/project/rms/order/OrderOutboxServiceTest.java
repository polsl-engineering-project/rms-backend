package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.order.event.OrderEvent;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceTest {

    @Mock
    OrderOutboxEventRepository repository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    OrderOutboxService underTest;

    private record DummyEvent(String foo) implements OrderEvent {}

    @Test
    @DisplayName("Given valid event_When persistEvent_Then saves serialized outbox event")
    void GivenValidEvent_WhenPersistEvent_ThenSavesSerializedOutboxEvent() throws Exception {
        //given
        var orderId = OrderId.generate();
        var event = new DummyEvent("bar");
        var expectedPayload = "{\"foo\":\"bar\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(expectedPayload);

        //when
        underTest.persistEvent(orderId, event);

        //then
        var captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(repository, times(1)).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(orderId.value());
        assertThat(saved.getType()).isEqualTo(event.getClass().getSimpleName());
        assertThat(saved.getPayload()).isEqualTo(expectedPayload);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given objectmapper throws JsonProcessingException_When persistEvent_Then wraps and rethrows OrderOutboxPersistenceException")
    void GivenObjectMapperThrows_WhenPersistEvent_ThenThrowsOrderOutboxPersistenceException() throws Exception {
        //given
        var orderId = OrderId.generate();
        var event = new DummyEvent("x");

        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("boom"){});

        //when / then
        assertThatThrownBy(() -> underTest.persistEvent(orderId, event))
                .isInstanceOf(com.polsl.engineering.project.rms.order.exception.OrderOutboxPersistenceException.class)
                .hasMessageContaining("Failed to serialize outbox event");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Given event_When persistEvent_Then type equals event simple class name")
    void GivenEvent_WhenPersistEvent_ThenTypeEqualsEventSimpleClassName() throws Exception {
        //given
        var orderId = OrderId.generate();
        var event = new DummyEvent("z");
        var expectedPayload = "payload";
        when(objectMapper.writeValueAsString(event)).thenReturn(expectedPayload);

        //when
        underTest.persistEvent(orderId, event);

        //then
        var captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved.getType()).isEqualTo(DummyEvent.class.getSimpleName());
    }

}

