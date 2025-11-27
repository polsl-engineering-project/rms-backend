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

import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

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
            LocalTime scheduledFor,
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

    public record  ApproveOrderRequest(
            @PositiveOrZero Integer estimatedPreparationMinutes
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
            Integer estimatedPreparationTimeMinutes,
            LocalDateTime approvedAt,
            LocalDateTime deliveryStartedAt
    ) {
    }

    public record OrderCustomerViewResponse(
            UUID id,
            String status,
            Integer estimatedPreparationMinutes,
            String cancellationReason,
            LocalDateTime approvedAt,
            LocalDateTime deliveryStartedAt
    ) {
    }

    @Builder
    public record OrderSearchRequest(
            List<com.polsl.engineering.project.rms.order.vo.OrderStatus> statuses,
            Instant placedFrom,
            Instant placedTo,
            String customerFirstName,
            DeliveryMode deliveryMode,
            OrderSortField sortBy,
            SortDirection sortDirection,
            Integer page,
            Integer size
    ){
        public OrderSearchRequest {
            if (page == null) page = 0;
            if (size == null) size = 20;
            if (sortBy == null) sortBy = OrderSortField.PLACED_AT;
            if (sortDirection == null) sortDirection = SortDirection.DESC;
        }
    }

    public record OrderSummaryResponse(
            UUID id,
            com.polsl.engineering.project.rms.order.vo.OrderStatus status,
            DeliveryMode deliveryMode,
            String customerFirstName,
            Instant placedAt,
            Instant updatedAt
    ){
    }

    public record OrderPageResponse(
            List<OrderSummaryResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            boolean hasPrevious,
            boolean hasNext
    ){
    }

    public enum OrderSortField {
        PLACED_AT,
        UPDATED_AT,
        DELIVERY_MODE,
        STATUS
    }

    public enum SortDirection {
        ASC,
        DESC
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
