package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.order.event.OrderEventType;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Integration tests for OrderOutboxEventRepository")
class OrderOutboxEventRepositoryIT extends ContainersEnvironment {

    @Autowired
    OrderOutboxEventRepository underTest;

    @Test
    @DisplayName("Given unprocessed and processed events_When findByProcessedIsFalse_Then returns only unprocessed slice")
    void GivenUnprocessedAndProcessedEvents_WhenFindByProcessedIsFalse_ThenReturnsOnlyUnprocessedSlice() {
        // given
        var e1 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "p1");
        var e2 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "p2");
        var e3 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "p3");
        // mark one as processed using package setter
        e3.setProcessed(true);

        underTest.saveAll(List.of(e1, e2, e3));

        // when
        Slice<OrderOutboxEvent> slice = underTest.findByProcessedIsFalse(PageRequest.of(0, 10));

        // then
        assertThat(slice).isNotNull();
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.getContent()).allSatisfy(ev -> assertThat(ev.isProcessed()).isFalse());
        assertThat(slice.getContent()).extracting(OrderOutboxEvent::getPayload).containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    @DisplayName("Given some processed events_When deleteByProcessedIsTrue_Then processed events are deleted")
    @Transactional
    void GivenSomeProcessedEvents_WhenDeleteByProcessedIsTrue_ThenProcessedEventsAreDeleted() {
        // given
        var e1 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "a");
        var e2 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "b");
        var e3 = OrderOutboxEvent.of(OrderId.generate(), OrderEventType.APPROVED_BY_FRONT_DESK, "c");
        e2.setProcessed(true);
        e3.setProcessed(true);

        underTest.saveAll(List.of(e1, e2, e3));

        // when
        underTest.deleteByProcessedIsTrue();

        // then
        var remaining = underTest.findAll();
        assertThat(remaining)
                .hasSize(1)
                .singleElement().satisfies(ev -> assertThat(ev.isProcessed()).isFalse());
    }

}