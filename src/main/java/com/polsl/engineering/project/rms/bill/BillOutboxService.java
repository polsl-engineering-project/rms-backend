package com.polsl.engineering.project.rms.bill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.bill.event.BillEvent;
import com.polsl.engineering.project.rms.bill.exception.BillOutboxPersistenceException;
import com.polsl.engineering.project.rms.bill.vo.BillId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class BillOutboxService {

    private final BillOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void persistEvent(BillId billId, BillEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            var outbox = BillOutboxEvent.of(billId, event.getType(), payload);
            repository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new BillOutboxPersistenceException("Failed to serialize outbox event", e);
        }
    }

}
