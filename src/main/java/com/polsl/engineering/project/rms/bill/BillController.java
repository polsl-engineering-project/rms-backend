package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.event.BillAddLinesEvent;
import com.polsl.engineering.project.rms.bill.event.BillClosedEvent;
import com.polsl.engineering.project.rms.bill.event.BillOpenedEvent;
import com.polsl.engineering.project.rms.bill.event.BillRemoveLinesEvent;
import com.polsl.engineering.project.rms.bill.vo.BillStatus;
import com.polsl.engineering.project.rms.general.error_handler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Tag(name = "Bill actions", description = "Operations related to bill management")
@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
class BillController {

    private final BillService billService;

    @Operation(summary = "Search and filter bills with pagination")
    @ApiResponse(responseCode = "200", description = "Bills retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillPayloads.BillPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    ResponseEntity<BillPayloads.BillPageResponse> searchBills(
            @RequestParam(name = "statuses", required = false) List<BillStatus> statuses,
            @RequestParam(name = "openedFrom", required = false) Instant openedFrom,
            @RequestParam(name = "openedTo", required = false) Instant openedTo,
            @RequestParam(name = "closedFrom", required = false) Instant closedFrom,
            @RequestParam(name = "closedTo", required = false) Instant closedTo,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "tableNumbers", required = false) List<Integer> tableNumbers,
            @RequestParam(name = "minTotalAmount", required = false) BigDecimal minTotalAmount,
            @RequestParam(name = "maxTotalAmount", required = false) BigDecimal maxTotalAmount,
            @RequestParam(name = "menuItemId", required = false) String menuItemId,
            @RequestParam(name = "sortBy", required = false) BillPayloads.BillSortField sortBy,
            @RequestParam(name = "sortDirection", required = false) BillPayloads.SortDirection sortDirection,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        var request = BillPayloads.BillSearchRequest.builder()
                .statuses(statuses)
                .openedFrom(openedFrom)
                .openedTo(openedTo)
                .closedFrom(closedFrom)
                .closedTo(closedTo)
                .userId(userId)
                .tableNumbers(tableNumbers)
                .minTotalAmount(minTotalAmount)
                .maxTotalAmount(maxTotalAmount)
                .menuItemId(menuItemId)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();
        var response = billService.searchBills(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search bill by id")
    @ApiResponse(responseCode = "200", description = "Bill retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillPayloads.BillSummaryWithLinesResponse.class)))
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    ResponseEntity<BillPayloads.BillSummaryWithLinesResponse> searchBill(@PathVariable("id") String id) {
        var response = billService.searchBill(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Open a new bill for a table")
    @ApiResponse(responseCode = "201", description = "Bill opened successfully",
            content = @Content(schema = @Schema(implementation = BillPayloads.BillOpenedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/open")
    ResponseEntity<BillPayloads.BillOpenedResponse> openBill(
            @Valid @RequestBody BillPayloads.OpenBillRequest request
    ) {
        var response = billService.openBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Add items to an open bill")
    @ApiResponse(responseCode = "204", description = "Items added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/add-items")
    ResponseEntity<Void> addItems(
            @PathVariable("id") String id,
            @Valid @RequestBody BillPayloads.AddItemsToBillRequest request
    ) {
        billService.addItems(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove items from an open bill")
    @ApiResponse(responseCode = "204", description = "Items removed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/remove-items")
    ResponseEntity<Void> removeItems(
            @PathVariable("id") String id,
            @Valid @RequestBody BillPayloads.RemoveItemsFromBillRequest request
    ) {
        billService.removeItems(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Close a bill")
    @ApiResponse(responseCode = "204", description = "Bill closed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/close")
    ResponseEntity<Void> closeBill(@PathVariable("id") String id) {
        billService.closeBill(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(
            summary = "WebSocket BillEvent Documentation",
            description = "Only for Bill WebSocket schemas exposition"
    )
    @ApiResponse(
            responseCode = "200",
            description = "WebSocket BillEvent schemas",
            content = @Content(array = @ArraySchema(schema = @Schema(oneOf = {
                    BillOpenedEvent.class,
                    BillAddLinesEvent.class,
                    BillRemoveLinesEvent.class,
                    BillClosedEvent.class
            }))
            ))
    @GetMapping("/docs/ws/bill-events")
    public void docs() {
        // ignore
    }
}