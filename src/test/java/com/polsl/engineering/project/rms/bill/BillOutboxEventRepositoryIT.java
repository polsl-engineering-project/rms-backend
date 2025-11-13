package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.bill.event.BillEventType;
import com.polsl.engineering.project.rms.bill.vo.BillId;
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
@DisplayName("Integration tests for BillOutboxEventRepository")
class BillOutboxEventRepositoryIT extends ContainersEnvironment {

    @Autowired
    BillOutboxEventRepository underTest;

    @Test
    @DisplayName("Given unprocessed and processed events_When findByProcessedIsFalse_Then returns only unprocessed slice")
    void GivenUnprocessedAndProcessedEvents_WhenFindByProcessedIsFalse_ThenReturnsOnlyUnprocessedSlice() {
        // given
        var e1 = BillOutboxEvent.of(BillId.generate(), BillEventType.OPENED, "payload1");
        var e2 = BillOutboxEvent.of(BillId.generate(), BillEventType.LINES_ADDED, "payload2");
        var e3 = BillOutboxEvent.of(BillId.generate(), BillEventType.CLOSED, "payload3");
        // mark one as processed using package setter
        e3.setProcessed(true);

        underTest.saveAll(List.of(e1, e2, e3));

        // when
        Slice<BillOutboxEvent> slice = underTest.findByProcessedIsFalse(PageRequest.of(0, 10));

        // then
        assertThat(slice).isNotNull();
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.getContent()).allSatisfy(ev -> assertThat(ev.isProcessed()).isFalse());
        assertThat(slice.getContent()).extracting(BillOutboxEvent::getPayload).containsExactlyInAnyOrder("payload1", "payload2");
    }

    @Test
    @DisplayName("Given some processed events_When deleteByProcessedIsTrue_Then processed events are deleted")
    @Transactional
    void GivenSomeProcessedEvents_WhenDeleteByProcessedIsTrue_ThenProcessedEventsAreDeleted() {
        // given
        var e1 = BillOutboxEvent.of(BillId.generate(), BillEventType.OPENED, "a");
        var e2 = BillOutboxEvent.of(BillId.generate(), BillEventType.LINES_REMOVED, "b");
        var e3 = BillOutboxEvent.of(BillId.generate(), BillEventType.CLOSED, "c");
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

    @Test
    @DisplayName("Given no unprocessed events_When findByProcessedIsFalse_Then returns empty slice")
    void GivenNoUnprocessedEvents_WhenFindByProcessedIsFalse_ThenReturnsEmptySlice() {
        // given
        var e1 = BillOutboxEvent.of(BillId.generate(), BillEventType.OPENED, "p1");
        var e2 = BillOutboxEvent.of(BillId.generate(), BillEventType.CLOSED, "p2");
        e1.setProcessed(true);
        e2.setProcessed(true);

        underTest.saveAll(List.of(e1, e2));

        // when
        Slice<BillOutboxEvent> slice = underTest.findByProcessedIsFalse(PageRequest.of(0, 10));

        // then
        assertThat(slice).isNotNull();
        assertThat(slice.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Given multiple unprocessed events_When findByProcessedIsFalse with page size_Then returns paginated results")
    void GivenMultipleUnprocessedEvents_WhenFindByProcessedIsFalseWithPageSize_ThenReturnsPaginatedResults() {
        // given
        var e1 = BillOutboxEvent.of(BillId.generate(), BillEventType.OPENED, "1");
        var e2 = BillOutboxEvent.of(BillId.generate(), BillEventType.LINES_ADDED, "2");
        var e3 = BillOutboxEvent.of(BillId.generate(), BillEventType.LINES_REMOVED, "3");
        var e4 = BillOutboxEvent.of(BillId.generate(), BillEventType.CLOSED, "4");

        underTest.saveAll(List.of(e1, e2, e3, e4));

        // when
        Slice<BillOutboxEvent> slice = underTest.findByProcessedIsFalse(PageRequest.of(0, 2));

        // then
        assertThat(slice).isNotNull();
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Given no processed events_When deleteByProcessedIsTrue_Then no events are deleted")
    @Transactional
    void GivenNoProcessedEvents_WhenDeleteByProcessedIsTrue_ThenNoEventsDeleted() {
        // given
        var e1 = BillOutboxEvent.of(BillId.generate(), BillEventType.OPENED, "x");
        var e2 = BillOutboxEvent.of(BillId.generate(), BillEventType.CLOSED, "y");

        underTest.saveAll(List.of(e1, e2));

        // when
        underTest.deleteByProcessedIsTrue();

        // then
        var remaining = underTest.findAll();
        assertThat(remaining).hasSize(2);
    }
}