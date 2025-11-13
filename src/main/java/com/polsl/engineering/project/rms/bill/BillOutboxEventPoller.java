package com.polsl.engineering.project.rms.bill;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BillOutboxEventPoller {

    private final BillOutboxEventRepository repository;
    private final BillOutboxEventProcessor processor;

    @Transactional
    @Scheduled(fixedDelayString = "${bill-outbox-event-poller.fixed-delay-ms:500}")
    void pollEvents() {
        var events = repository.findByProcessedIsFalse(
                PageRequest.of(0, 10)
        );
        events.forEach(processor::processEvent);
    }

}
