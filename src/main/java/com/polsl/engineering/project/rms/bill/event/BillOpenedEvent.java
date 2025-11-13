package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.TableNumber;
import java.time.Instant;
import java.util.List;

public record BillOpenedEvent(
        BillId billId,
        TableNumber tableNumber,
        List<BillLine> initialLines,
        Instant openedAt
) implements BillEvent {
    @Override
    public BillEventType getType() {
        return BillEventType.OPENED;
    }

    @Override
    public Instant getOccurredAt() {
        return openedAt;
    }
}
