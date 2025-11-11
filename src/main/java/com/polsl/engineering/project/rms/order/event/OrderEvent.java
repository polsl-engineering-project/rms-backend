package com.polsl.engineering.project.rms.order.event;

import java.time.Instant;

/**
 * Marker interface for order domain events.
 */
public interface OrderEvent {
    OrderEventType getType();
    Instant getOccurredAt();
}
