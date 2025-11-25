package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.TableNumber;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "BillOpenEvent schema, type: OPENED")
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
