package com.polsl.engineering.project.rms.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderOutboxEventCleaner {

    private final OrderOutboxEventRepository repository;

    @Transactional
    @Scheduled(fixedDelayString = "${order-outbox-event-cleaner.fixed-delay-ms:3600000}")
    void clean() {
        repository.deleteByProcessedIsTrue();
    }

}
