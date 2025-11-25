package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLineRemoval;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "BillRemoveLinesEvent schema, type: LINES_REMOVED")
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
