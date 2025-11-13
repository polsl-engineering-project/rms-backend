package com.polsl.engineering.project.rms.bill;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BillOutboxEventCleaner {

    private final BillOutboxEventRepository repository;

    @Transactional
    @Scheduled(fixedDelayString = "${bill-outbox-event-cleaner.fixed-delay-ms:3600000}")
    void clean() {
        repository.deleteByProcessedIsTrue();
    }

}
