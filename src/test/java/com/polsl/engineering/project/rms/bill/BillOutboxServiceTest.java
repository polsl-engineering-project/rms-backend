package com.polsl.engineering.project.rms.bill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.bill.event.BillEvent;
import com.polsl.engineering.project.rms.bill.event.BillEventType;
import com.polsl.engineering.project.rms.bill.vo.BillId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillOutboxServiceTest {

    @Mock
    BillOutboxEventRepository repository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    BillOutboxService underTest;

    private record DummyEvent(String data) implements BillEvent {
        @Override
        public BillEventType getType() {
            return BillEventType.OPENED;
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }
    }

    @Test
    @DisplayName("Given valid event_When persistEvent_Then saves serialized outbox event")
    void GivenValidEvent_WhenPersistEvent_ThenSavesSerializedOutboxEvent() throws Exception {
        //given
        var billId = BillId.generate();
        var event = new DummyEvent("test-data");
        var expectedPayload = "{\"data\":\"test-data\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(expectedPayload);

        //when
        underTest.persistEvent(billId, event);

        //then
        var captor = ArgumentCaptor.forClass(BillOutboxEvent.class);
        verify(repository, times(1)).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved).isNotNull();
        assertThat(saved.getBillId()).isEqualTo(billId.value());
        assertThat(saved.getType()).isEqualTo(BillEventType.OPENED);
        assertThat(saved.getPayload()).isEqualTo(expectedPayload);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given objectmapper throws JsonProcessingException_When persistEvent_Then wraps and rethrows BillOutboxPersistenceException")
    void GivenObjectMapperThrows_WhenPersistEvent_ThenThrowsBillOutboxPersistenceException() throws Exception {
        //given
        var billId = BillId.generate();
        var event = new DummyEvent("invalid");

        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("serialization failed"){});

        //when / then
        assertThatThrownBy(() -> underTest.persistEvent(billId, event))
                .isInstanceOf(com.polsl.engineering.project.rms.bill.exception.BillOutboxPersistenceException.class)
                .hasMessageContaining("Failed to serialize outbox event");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Given event_When persistEvent_Then type equals event type")
    void GivenEvent_WhenPersistEvent_ThenTypeEqualsEventType() throws Exception {
        //given
        var billId = BillId.generate();
        var event = new DummyEvent("sample");
        var expectedPayload = "payload";
        when(objectMapper.writeValueAsString(event)).thenReturn(expectedPayload);

        //when
        underTest.persistEvent(billId, event);

        //then
        var captor = ArgumentCaptor.forClass(BillOutboxEvent.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved.getType()).isEqualTo(BillEventType.OPENED);
    }
}