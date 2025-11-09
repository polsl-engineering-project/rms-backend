package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.vo.BillStatus;
import com.polsl.engineering.project.rms.bill.vo.Money;
import com.polsl.engineering.project.rms.bill.vo.WaiterInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    public record BillOpenedResponse(
            UUID id,
            Integer tableNumber
    ) {
    }

    @Builder
    public record BillSearchRequest(
            List<BillStatus> statuses,
            LocalDate openedFrom,
            LocalDate openedTo,
            LocalDate closedFrom,
            LocalDate closedTo,
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
            String menuItemId,
            String searchText,
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

        public BillSearchCriteria toCriteria() {
            return BillSearchCriteria.builder()
                    .statuses(statuses)
                    .openedFrom(openedFrom)
                    .openedTo(openedTo)
                    .closedFrom(closedFrom)
                    .closedTo(closedTo)
                    .waiterEmployeeId(waiterEmployeeId)
                    .waiterFirstName(waiterFirstName)
                    .waiterLastName(waiterLastName)
                    .tableNumber(tableNumber)
                    .tableNumbers(tableNumbers)
                    .minTotalAmount(minTotalAmount)
                    .maxTotalAmount(maxTotalAmount)
                    .menuItemId(menuItemId)
                    .searchText(searchText)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
        }
    }

    record BillSummary(
            UUID id,
            Integer tableNumber,
            BillStatus status,
            String waiterName,
            String waiterEmployeeId,
            Money totalAmount,
            Integer itemCount,
            Instant openedAt,
            Instant closedAt,
            Instant updatedAt
    ) {
    }

    public record BillSummaryResponse(
            UUID id,
            Integer tableNumber,
            BillStatus status,
            String waiterName,
            String waiterEmployeeId,
            BigDecimal totalAmount,
            Integer itemCount,
            Instant openedAt,
            Instant closedAt,
            Instant updatedAt
    ) {
        static BillSummaryResponse from(BillSummary summary) {
            return new BillSummaryResponse(
                    summary.id(),
                    summary.tableNumber(),
                    summary.status(),
                    summary.waiterName(),
                    summary.waiterEmployeeId(),
                    summary.totalAmount().amount(),
                    summary.itemCount(),
                    summary.openedAt(),
                    summary.closedAt(),
                    summary.updatedAt()
            );
        }
    }

    record BillPage(
            List<BillSummary> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages
    ) {
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
        static BillPageResponse from(BillPage page) {
            var content = page.content().stream()
                    .map(BillSummaryResponse::from)
                    .toList();

            return new BillPageResponse(
                    content,
                    page.pageNumber(),
                    page.pageSize(),
                    page.totalElements(),
                    page.totalPages(),
                    page.pageNumber() == 0,
                    page.pageNumber() == page.totalPages() - 1,
                    page.pageNumber() > 0,
                    page.pageNumber() < page.totalPages() - 1
            );
        }
    }

    @Getter
    @Builder
    public class BillSearchCriteria {
        private List<BillStatus> statuses;
        private LocalDate openedFrom;
        private LocalDate openedTo;
        private LocalDate closedFrom;
        private LocalDate closedTo;
        private Instant openedAfter;
        private Instant openedBefore;
        private Instant closedAfter;
        private Instant closedBefore;
        private String waiterEmployeeId;
        private String waiterFirstName;
        private String waiterLastName;
        private Integer tableNumber;
        private List<Integer> tableNumbers;
        private BigDecimal minTotalAmount;
        private BigDecimal maxTotalAmount;
        private String menuItemId;
        private String searchText;
        private BillSortField sortBy;
        private SortDirection sortDirection;

    }

    public enum BillSortField {
        OPENED_AT,
        CLOSED_AT,
        UPDATED_AT,
        TOTAL_AMOUNT,
        TABLE_NUMBER,
        WAITER_LAST_NAME
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}