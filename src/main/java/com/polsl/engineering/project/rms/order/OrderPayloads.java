package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class OrderPayloads {

    public record OrderLine(
            UUID menuItemId,
            int quantity,
            long version
    ) {
    }

    public record PlaceDeliveryOrderRequest(
            CustomerInfo customerInfo,
            Address address,
            DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            List<OrderLine> orderLines
    ) {
    }

    public record PlacePickUpOrderRequest(
            CustomerInfo customerInfo,
            DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            List<OrderLine> orderLines
    ) {
    }

    public record CancelOrderRequest(
            String reason
    ) {
    }

    public record AddItemsByStaffRequest(
            List<OrderLine> orderLines,
            Integer updatedEstimatedMinutes
    ) {
    }

    public record  ApproveOrderByKitchenRequest(
            Integer updatedEstimatedMinutes
    ) {
    }

    public record OrderPlacedResponse(
            UUID id
    ) {
    }

}
