package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.error_handler.ErrorResponse;
import com.polsl.engineering.project.rms.order.event.*;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import com.polsl.engineering.project.rms.security.UserPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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

    @Operation(summary = "Approve order")
    @ApiResponse(responseCode = "204", description = "Order approved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/approve")
    ResponseEntity<Void> approve(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.ApproveOrderRequest request
    ) {
        orderService.approve(id, request);
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
    ResponseEntity<Void> completeOrder(@PathVariable("id") String id, @AuthenticationPrincipal UserPrincipal user) {
        orderService.completeOrder(id, user);
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

    @Operation(summary = "Get limited view of your order (customer)")
    @ApiResponse(responseCode = "200", description = "Order view retrieved successfully",
            content = @Content(schema = @Schema(implementation = OrderPayloads.OrderCustomerViewResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}/customer-view")
    ResponseEntity<OrderPayloads.OrderCustomerViewResponse> getOrderForCustomer(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderForCustomer(id));
    }

    @Operation(summary = "Search and filter orders with pagination")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
            content = @Content(schema = @Schema(implementation = OrderPayloads.OrderPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    ResponseEntity<OrderPayloads.OrderPageResponse> searchOrders(
            @RequestParam(name = "statuses", required = false) List<OrderStatus> statuses,
            @RequestParam(name = "placedFrom", required = false) Instant placedFrom,
            @RequestParam(name = "placedTo", required = false) Instant placedTo,
            @RequestParam(name = "customerFirstName", required = false) String customerFirstName,
            @RequestParam(name = "deliveryMode", required = false) DeliveryMode deliveryMode,
            @RequestParam(name = "sortBy", required = false) OrderPayloads.OrderSortField sortBy,
            @RequestParam(name = "sortDirection", required = false) OrderPayloads.SortDirection sortDirection,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        var request = OrderPayloads.OrderSearchRequest.builder()
                .statuses(statuses)
                .placedFrom(placedFrom)
                .placedTo(placedTo)
                .customerFirstName(customerFirstName)
                .deliveryMode(deliveryMode)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();
        var response = orderService.searchOrders(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "WebSocket OrderEvent Documentation",
            description = "Only for Event WebSocket schemas exposition"
    )
    @ApiResponse(
            responseCode = "200",
            description = "WebSocket OrderEvent schemas",
            content = @Content(array = @ArraySchema(schema = @Schema(oneOf = {
                    DeliveryOrderPlacedEvent.class,
                    PickUpOrderPlacedEvent.class,
                    OrderApprovedEvent.class,
                    OrderCancelledEvent.class,
                    OrderCompletedEvent.class,
                    OrderDeliveryStartedEvent.class,
                    OrderLinesChangedEvent.class,
                    OrderMarkedAsReadyEvent.class,
                    OrderInitialDataEvent.class
            }))
            ))
    @GetMapping("/docs/ws/order-events")
    public void docs() {
        // ignore
    }


}
