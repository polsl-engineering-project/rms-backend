package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderDeliveryStartedEvent schema, type: DELIVERY_STARTED")
public record OrderDeliveryStartedEvent(
        OrderId orderId,
        Instant startedAt
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.DELIVERY_STARTED;
    }

    @Override
    public Instant getOccurredAt() {
        return startedAt();
    }
}
