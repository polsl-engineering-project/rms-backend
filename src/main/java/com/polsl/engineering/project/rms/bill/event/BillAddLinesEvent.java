package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "BillAddLinesEvent schema, type: LINES_ADDED")
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
