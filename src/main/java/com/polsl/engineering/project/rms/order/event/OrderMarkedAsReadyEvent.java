package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderMarkedAsReadyEvent schema, type: MARKED_AS_READY")
public record OrderMarkedAsReadyEvent(
        OrderId orderId,
        Instant readyAt,
        OrderStatus readyStatus
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.MARKED_AS_READY;
    }

    @Override
    public Instant getOccurredAt() {
        return readyAt();
    }
}
