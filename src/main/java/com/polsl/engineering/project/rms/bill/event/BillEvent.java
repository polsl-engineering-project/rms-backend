package com.polsl.engineering.project.rms.bill.event;

import java.time.Instant;

public interface BillEvent {
    BillEventType getType();
    Instant getOccurredAt();
}
