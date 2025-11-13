package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;

import java.time.Instant;

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
