package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.common.error_handler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bill actions", description = "Operations related to bill management")
@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
class BillController {

    private final BillService billService;

    @Operation(summary = "Search and filter bills with pagination")
    @ApiResponse(responseCode = "200", description = "Bills retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillPayloads.BillPageResponse.class)))
    @PostMapping
    ResponseEntity<BillPayloads.BillPageResponse> searchBills(@RequestBody BillPayloads.BillSearchRequest request) {
        var response = billService.searchBills(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search bill by id")
    @ApiResponse(responseCode = "200", description = "Bill retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillPayloads.BillSummaryResponse.class)))
    @GetMapping("{id}")
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
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/close")
    ResponseEntity<Void> closeBill(@PathVariable("id") String id) {
        billService.closeBill(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Pay a bill")
    @ApiResponse(responseCode = "204", description = "Bill payed successfully")
    @ApiResponse(responseCode = "404", description = "Bill not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/pay")
    ResponseEntity<Void> payBill(
            @PathVariable("id") String id,
            @Valid @RequestBody BillPayloads.PayBillRequest payBillRequest) {
        billService.payBill(id, payBillRequest);
        return ResponseEntity.noContent().build();
    }
}