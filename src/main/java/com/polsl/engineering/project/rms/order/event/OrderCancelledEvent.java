package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderCancelledEvent schema, type: CANCELLED")
public record OrderCancelledEvent(
        OrderId orderId,
        Instant cancelledAt,
        String reason
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.CANCELLED;
    }

    @Override
    public Instant getOccurredAt() {
        return cancelledAt();
    }
}
