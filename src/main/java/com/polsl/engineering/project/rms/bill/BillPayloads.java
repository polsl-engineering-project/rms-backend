package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.vo.BillStatus;
import com.polsl.engineering.project.rms.bill.vo.WaiterInfo;
import com.polsl.engineering.project.rms.order.vo.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BillPayloads {

    public record BillLine(
            @NotNull UUID menuItemId,
            @Min(1) int quantity,
            @PositiveOrZero long version
    ) {
    }

    public record OpenBillRequest(
            @NotNull @Min(1) Integer tableNumber,
            @Valid @NotNull WaiterInfo waiterInfo,
            @NotEmpty @Valid List<BillLine> initialLines
    ) {
    }

    public record AddItemsToBillRequest(
            @NotEmpty @Valid List<BillLine> newLines
    ) {
    }

    public record RemoveLine(
            @NotNull UUID menuItemId,
            @Min(1) int quantity
    ) {
    }

    public record RemoveItemsFromBillRequest(
            @NotEmpty @Valid List<RemoveLine> removedLines
    ) {
    }

    public record PayBillRequest(
            @NotNull PaymentMethod paymentMethod,
            @NotNull Double paidAmount
    ){
    }

    public record BillOpenedResponse(
            UUID id,
            Integer tableNumber
    ) {
    }

    @Builder
    public record BillSearchRequest(
            List<BillStatus> statuses,
            Instant openedFrom,
            Instant openedTo,
            Instant closedFrom,
            Instant closedTo,
            Instant paidFrom,
            Instant paidTo,
            PaymentMethod paymentMethod,
            String waiterEmployeeId,
            String waiterFirstName,
            String waiterLastName,
            @Min(1)
            Integer tableNumber,
            List<@Min(1) Integer> tableNumbers,
            @PositiveOrZero
            BigDecimal minTotalAmount,
            @PositiveOrZero
            BigDecimal maxTotalAmount,
            @PositiveOrZero
            BigDecimal minPaidAmount,
            @PositiveOrZero
            BigDecimal maxPaidAmount,
            String menuItemId,
            BillSortField sortBy,
            SortDirection sortDirection,
            @Min(0)
            Integer page,
            @Min(1) @Max(100)
            Integer size
    ) {
        public BillSearchRequest {
            if (page == null) page = 0;
            if (size == null) size = 20;
            if (sortBy == null) sortBy = BillSortField.OPENED_AT;
            if (sortDirection == null) sortDirection = SortDirection.DESC;
        }
    }


    public record BillSummaryResponse(
            UUID id,
            Integer tableNumber,
            BillStatus status,
            PaymentMethod paymentMethod,
            String waiterName,
            String waiterEmployeeId,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            Integer itemCount,
            Instant openedAt,
            Instant closedAt,
            Instant paidAt,
            Instant updatedAt
    ) {
    }

    public record BillSummaryWithLinesResponse(
            UUID id,
            Integer tableNumber,
            BillStatus status,
            PaymentMethod paymentMethod,
            String waiterName,
            String waiterEmployeeId,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            List<BillLineResponse> billLines,
            Instant openedAt,
            Instant closedAt,
            Instant paidAt,
            Instant updatedAt
    ) {

    }

    public record BillLineResponse(
            UUID menuItemId,
            int quantity,
            String name,
            long version
    ){
    }

    public record BillPageResponse(
            List<BillSummaryResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            boolean hasPrevious,
            boolean hasNext
    ) {
    }

    public enum BillSortField {
        OPENED_AT,
        CLOSED_AT,
        PAID_AT,
        UPDATED_AT,
        TOTAL_AMOUNT,
        PAID_AMOUNT,
        TABLE_NUMBER,
        WAITER_LAST_NAME
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}