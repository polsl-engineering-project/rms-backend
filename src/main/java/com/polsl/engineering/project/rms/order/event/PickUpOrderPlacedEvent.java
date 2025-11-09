package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public record PickUpOrderPlacedEvent(
        OrderId orderId,
        Instant placedAt,
        List<OrderLine> lines,
        Address deliveryAddress,
        CustomerInfo customerInfo,
        LocalTime scheduledFor
) implements OrderEvent {
}

