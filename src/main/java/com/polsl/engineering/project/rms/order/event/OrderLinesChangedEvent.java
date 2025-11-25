package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderLineRemoval;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "OrderLinesChangedEvent schema, type: LINES_CHANGED")
public record OrderLinesChangedEvent(
        OrderId orderId,
        Instant changedAt,
        List<OrderLine> addedLines,
        List<OrderLineRemoval> removedLines,
        Integer updatedEstimatedPreparationMinutes
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.LINES_CHANGED;
    }

    @Override
    public Instant getOccurredAt() {
        return changedAt();
    }
}
