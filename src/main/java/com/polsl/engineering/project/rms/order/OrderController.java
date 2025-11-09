package com.polsl.engineering.project.rms.order;

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

@Tag(name = "Order actions", description = "Operations related to order management")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place a pick-up order")
    @ApiResponse(responseCode = "201", description = "Order placed successfully",
            content = @Content(schema = @Schema(implementation = OrderPayloads.OrderPlacedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/place-pick-up-order")
    ResponseEntity<OrderPayloads.OrderPlacedResponse> placePickUpOrder(
            @Valid @RequestBody OrderPayloads.PlacePickUpOrderRequest request
    ) {
        var response = orderService.placePickUpOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Place a delivery order")
    @ApiResponse(responseCode = "201", description = "Order placed successfully",
            content = @Content(schema = @Schema(implementation = OrderPayloads.OrderPlacedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/place-delivery-order")
    ResponseEntity<OrderPayloads.OrderPlacedResponse> placeDeliveryOrder(
            @Valid @RequestBody OrderPayloads.PlaceDeliveryOrderRequest request
    ) {
        var response = orderService.placeDeliveryOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Approve order by front desk")
    @ApiResponse(responseCode = "204", description = "Order approved successfully")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/approve/front-desk")
    ResponseEntity<Void> approveByFrontDesk(@PathVariable("id") String id) {
        orderService.approveByFrontDesk(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Approve order by kitchen")
    @ApiResponse(responseCode = "204", description = "Order approved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/approve/kitchen")
    ResponseEntity<Void> approveByKitchen(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.ApproveOrderByKitchenRequest request
    ) {
        orderService.approveByKitchen(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark order as ready")
    @ApiResponse(responseCode = "204", description = "Order marked as ready")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/ready")
    ResponseEntity<Void> markAsReady(@PathVariable("id") String id) {
        orderService.markAsReady(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Start delivery for an order")
    @ApiResponse(responseCode = "204", description = "Delivery started")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/start-delivery")
    ResponseEntity<Void> startDelivery(@PathVariable("id") String id) {
        orderService.startDelivery(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Complete an order")
    @ApiResponse(responseCode = "204", description = "Order completed")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/complete")
    ResponseEntity<Void> completeOrder(@PathVariable("id") String id) {
        orderService.completeOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel an order")
    @ApiResponse(responseCode = "204", description = "Order cancelled")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/cancel")
    ResponseEntity<Void> cancelOrder(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.CancelOrderRequest request
    ) {
        orderService.cancelOrder(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change order lines")
    @ApiResponse(responseCode = "204", description = "Order lines changed")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/change-lines")
    ResponseEntity<Void> changeOrderLines(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.ChangeOrderLinesRequest request
    ) {
        orderService.changeOrderLines(id, request);
        return ResponseEntity.noContent().build();
    }

}
