package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLineRemoval;

import java.time.Instant;
import java.util.List;

public record BillRemoveLinesEvent(
        BillId billId,
        List<BillLineRemoval> removedLines,
        Instant updatedAt

) implements BillEvent {
    @Override
    public BillEventType getType() {
        return BillEventType.LINES_REMOVED;
    }

    @Override
    public Instant getOccurredAt() {
        return updatedAt;
    }
}
