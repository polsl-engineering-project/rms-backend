package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "BillClosedEvent schema, type: CLOSED")
public record BillClosedEvent(
        BillId billId,
        Instant closedAt
) implements BillEvent {
    @Override
    public BillEventType getType() {
        return BillEventType.CLOSED;
    }

    @Override
    public Instant getOccurredAt() {
        return closedAt;
    }
}
