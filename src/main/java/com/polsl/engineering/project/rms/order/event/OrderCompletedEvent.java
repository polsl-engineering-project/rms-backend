package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderCompletedEvent schema, type: COMPLETED")
public record OrderCompletedEvent(
        OrderId orderId,
        Instant completedAt
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.COMPLETED;
    }

    @Override
    public Instant getOccurredAt() {
        return completedAt();
    }
}
