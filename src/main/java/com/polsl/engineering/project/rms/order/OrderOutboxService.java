package com.polsl.engineering.project.rms.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polsl.engineering.project.rms.order.event.OrderEvent;
import com.polsl.engineering.project.rms.order.exception.OrderOutboxPersistenceException;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

    private final OrderOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void persistEvent(OrderId orderId, OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            var outbox = OrderOutboxEvent.of(orderId, event.getType().name(), payload);
            repository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new OrderOutboxPersistenceException("Failed to serialize outbox event", e);
        }
    }

}
