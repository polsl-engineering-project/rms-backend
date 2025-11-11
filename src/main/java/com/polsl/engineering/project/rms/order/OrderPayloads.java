package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderPayloads {

    public record OrderLine(
            @NotNull UUID menuItemId,
            @Min(1) int quantity,
            @PositiveOrZero long version
    ) {
    }

    public record PlaceDeliveryOrderRequest(
            @Valid @NotNull CustomerInfo customerInfo,
            @Valid @NotNull Address address,
            @NotNull DeliveryMode deliveryMode,
            @NotNull LocalTime scheduledFor,
            @NotEmpty @Valid List<OrderLine> orderLines
    ) {
    }

    public record PlacePickUpOrderRequest(
            @Valid @NotNull CustomerInfo customerInfo,
            @NotNull DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            @NotEmpty @Valid List<OrderLine> orderLines
    ) {
    }

    public record CancelOrderRequest(
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record AddItemsByStaffRequest(
            @NotEmpty @Valid List<OrderLine> orderLines,
            @PositiveOrZero Integer updatedEstimatedMinutes
    ) {
    }

    public record  ApproveOrderByKitchenRequest(
            @PositiveOrZero Integer updatedEstimatedMinutes
    ) {
    }

    public record RemoveLine(
            @NotNull UUID menuItemId,
            @Min(1) int quantity
    ) {
    }

    public record ChangeOrderLinesRequest(
            @Valid List<OrderLine> newLines,
            @Valid List<RemoveLine> removedLines,
            @PositiveOrZero Integer updatedEstimatedPreparationTimeMinutes
    ) {
    }

    public record OrderPlacedResponse(
            UUID id
    ) {
    }

    public record OrderDetailsResponse(
            UUID id,
            String status,
            CustomerInfo customerInfo,
            Address address,
            DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            List<OrderLine> orderLines,
            Integer estimatedPreparationTimeMinutes
    ) {
    }

    public record OrderCustomerViewResponse(
            UUID id,
            String status,
            Integer estimatedPreparationMinutes,
            String cancellationReason
    ) {
    }

    public record OrderWebsocketMessage(
            String type,
            Object data
    ) {
        static OrderWebsocketMessage error(String errorMessage) {
            return new OrderWebsocketMessage("ERROR", Map.of("message", errorMessage));
        }
    }

}
