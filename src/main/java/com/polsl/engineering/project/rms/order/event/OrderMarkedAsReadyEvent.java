package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;

import java.time.Instant;

public record OrderMarkedAsReadyEvent(
        OrderId orderId,
        Instant readyAt,
        OrderStatus readyStatus
) implements OrderEvent {
}
