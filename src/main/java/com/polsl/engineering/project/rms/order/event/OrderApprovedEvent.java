package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderApprovedByKitchenEvent schema, type: APPROVED_BY_KITCHEN")
public record OrderApprovedEvent(
        OrderId orderId,
        Instant approvedAt,
        Integer estimatedPreparationMinutes
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.APPROVED;
    }

    @Override
    public Instant getOccurredAt() {
        return approvedAt;
    }
}
