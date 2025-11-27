package com.polsl.engineering.project.rms.order.event;

import com.polsl.engineering.project.rms.order.OrderPayloads;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
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
        List<OrderPayloads.OrderLineResponse> orderLines,
        Instant placedAt,
        Integer estimatedPreparationTimeMinutes,
        LocalDateTime approvedAt,
        LocalDateTime deliveryStartedAt
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
