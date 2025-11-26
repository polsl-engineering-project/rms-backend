package com.polsl.engineering.project.rms.bill.event;

import com.polsl.engineering.project.rms.bill.BillPayloads;
import com.polsl.engineering.project.rms.bill.vo.BillStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "BillInitialDataEvent schema, type: INITIAL_DATA")
public record BillInitialDataEvent(
        UUID id,
        Integer tableNumber,
        BillStatus status,
        String userId,
        BigDecimal totalAmount,
        List<BillPayloads.BillLineResponse> billLines,
        Instant openedAt,
        Instant closedAt,
        Instant updatedAt
) implements BillEvent {
    @Override
    public BillEventType getType() {
        return BillEventType.INITIAL_DATA;
    }

    @Override
    public Instant getOccurredAt() {
        return openedAt;
    }
}
