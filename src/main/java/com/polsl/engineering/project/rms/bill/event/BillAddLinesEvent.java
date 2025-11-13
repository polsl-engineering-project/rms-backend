package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import java.time.Instant;
import java.util.List;

public record BillAddLinesEvent(
        BillId billId,
        List<BillLine> newLines,
        Instant updatedAt
) implements BillEvent {
    @Override
    public BillEventType getType() {
        return BillEventType.LINES_ADDED;
    }

    @Override
    public Instant getOccurredAt() {
        return updatedAt;
    }
}
