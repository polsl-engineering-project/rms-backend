package com.polsl.engineering.project.rms.order;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.sql.Time;
import java.util.Objects;

@Component
class OrderQueryParamsBuilder {

    Object[] buildInsertParams(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        var id = order.getId().value();
        var common = buildCommonParams(order);
        var params = new Object[common.length + 2];
        params[0] = id;
        System.arraycopy(common, 0, params, 1, common.length);
        params[params.length - 1] = order.getVersion();
        return params;
    }

    Object[] buildUpdateParams(Order order) {
        Objects.requireNonNull(order, "order cannot be null");
        var id = order.getId().value();
        var common = buildCommonParams(order);
        var params = new Object[common.length + 2];
        System.arraycopy(common, 0, params, 0, common.length);
        params[common.length] = id;
        params[common.length + 1] = order.getVersion();
        return params;
    }

    private Object[] buildCommonParams(Order order) {
        var addr = order.getDeliveryAddress();
        var scheduledFor = order.getScheduledFor() == null ? null : Time.valueOf(order.getScheduledFor());
        var placedAt = order.getPlacedAt() == null ? null : Timestamp.from(order.getPlacedAt());
        var updatedAt = order.getUpdatedAt() == null ? null : Timestamp.from(order.getUpdatedAt());
        var approvedAt = order.getApprovedAt() == null ? null : Timestamp.valueOf(order.getApprovedAt());
        var deliveryStartedAt = order.getDeliveryStartedAt() == null ? null : Timestamp.valueOf(order.getDeliveryStartedAt());

        return new Object[]{
                order.getType().name(),
                order.getDeliveryMode().name(),
                order.getStatus().name(),
                addr == null ? null : addr.street(),
                addr == null ? null : addr.houseNumber(),
                addr == null ? null : addr.apartmentNumber(),
                addr == null ? null : addr.city(),
                addr == null ? null : addr.postalCode(),
                order.getCustomerInfo().firstName(),
                order.getCustomerInfo().lastName(),
                order.getCustomerInfo().phoneNumber(),
                scheduledFor,
                order.getEstimatedPreparationMinutes(),
                order.getCancellationReason(),
                approvedAt,
                deliveryStartedAt,
                placedAt,
                updatedAt
        };
    }
}
