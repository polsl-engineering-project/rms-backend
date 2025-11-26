package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.OrderPayloads;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "OrderInitialDataEvent schema, type: INITIAL_DATA")
public record OrderInitialDataEvent(
        UUID id,
        String status,
        CustomerInfo customerInfo,
        Address address,
        DeliveryMode deliveryMode,
        LocalTime scheduledFor,
        Instant placedAt,
        List<OrderPayloads.OrderLine> orderLines,
        Integer estimatedPreparationTimeMinutes
) implements OrderEvent {
    @Override
    public OrderEventType getType() {
        return OrderEventType.INITIAL_DATA;
    }

    @Override
    public Instant getOccurredAt() {
        return placedAt;
    }
}
