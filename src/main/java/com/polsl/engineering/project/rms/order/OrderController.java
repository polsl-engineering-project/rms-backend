package com.polsl.engineering.project.rms.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @PostMapping("/place-pick-up-order")
    ResponseEntity<OrderPayloads.OrderPlacedResponse> placePickUpOrder(
            @Valid @RequestBody OrderPayloads.PlacePickUpOrderRequest request
    ) {
        var response = orderService.placePickUpOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/place-delivery-order")
    ResponseEntity<OrderPayloads.OrderPlacedResponse> placeDeliveryOrder(
            @Valid @RequestBody OrderPayloads.PlaceDeliveryOrderRequest request
    ) {
        var response = orderService.placeDeliveryOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/approve/front-desk")
    ResponseEntity<Void> approveByFrontDesk(@PathVariable("id") String id) {
        orderService.approveByFrontDesk(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve/kitchen")
    ResponseEntity<Void> approveByKitchen(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.ApproveOrderByKitchenRequest request
    ) {
        orderService.approveByKitchen(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ready")
    ResponseEntity<Void> markAsReady(@PathVariable("id") String id) {
        orderService.markAsReady(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start-delivery")
    ResponseEntity<Void> startDelivery(@PathVariable("id") String id) {
        orderService.startDelivery(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    ResponseEntity<Void> completeOrder(@PathVariable("id") String id) {
        orderService.completeOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    ResponseEntity<Void> cancelOrder(
            @PathVariable("id") String id,
            @Valid @RequestBody OrderPayloads.CancelOrderRequest request
    ) {
        orderService.cancelOrder(id, request);
        return ResponseEntity.noContent().build();
    }

}
