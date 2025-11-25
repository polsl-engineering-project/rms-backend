package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "OrderApprovedByFrontDeskEvent schema, type: APPROVED_BY_FRONT_DESK")
public record OrderApprovedByFrontDeskEvent(
        OrderId orderId,
        Instant approvedAt
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.APPROVED_BY_FRONT_DESK;
    }

    @Override
    public Instant getOccurredAt() {
        return approvedAt();
    }
}
