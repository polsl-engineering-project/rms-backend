package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "DeliveryOrderPlacedEvent schema, type: DELIVERY_ORDER_PLACED")
public record DeliveryOrderPlacedEvent(
        OrderId orderId,
        Instant placedAt,
        List<OrderLine> lines,
        Address deliveryAddress,
        CustomerInfo customerInfo,
        LocalTime scheduledFor
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.DELIVERY_ORDER_PLACED;
    }

    @Override
    public Instant getOccurredAt() {
        return placedAt();
    }
}
