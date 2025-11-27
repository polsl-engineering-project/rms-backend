package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.result.Result;
import com.polsl.engineering.project.rms.order.cmd.*;
import com.polsl.engineering.project.rms.order.event.OrderEvent;
import com.polsl.engineering.project.rms.order.event.*;
import com.polsl.engineering.project.rms.order.vo.*;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class Order {

    private OrderId id;

    private OrderType type;

    private DeliveryMode deliveryMode;

    private OrderStatus status;

    @Getter(AccessLevel.NONE)
    private final List<OrderLine> lines = new ArrayList<>();

    private Address deliveryAddress;

    private CustomerInfo customerInfo;

    private LocalTime scheduledFor;

    private Integer estimatedPreparationMinutes;

    private String cancellationReason;

    private Instant placedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime deliveryStartedAt;

    private Instant updatedAt;

    private long version;

    @Getter(AccessLevel.NONE)
    private final List<OrderEvent> events = new ArrayList<>();

    // ==== Constructors ====
    private Order(
            OrderType orderType,
            DeliveryMode deliveryMode,
            List<OrderLine> orderLines,
            Address deliveryAddress,
            CustomerInfo customerInfo,
            LocalTime scheduledFor,
            Clock clock
    ) {
        this.id = OrderId.generate();
        this.type = orderType;
        this.deliveryMode = deliveryMode;
        this.status = OrderStatus.PENDING_APPROVAL;
        this.lines.addAll(orderLines);
        this.deliveryAddress = deliveryAddress;
        this.customerInfo = customerInfo;
        this.scheduledFor = scheduledFor;
        this.placedAt = Instant.now(clock);
        this.updatedAt = Instant.now(clock);
        this.version = 0;
        groupOrderLines();
    }

    // ==== Factory Methods ====
    static Order reconstruct(
            OrderId id,
            OrderType type,
            DeliveryMode deliveryMode,
            OrderStatus status,
            List<OrderLine> lines,
            Address deliveryAddress,
            CustomerInfo customerInfo,
            LocalTime scheduledFor,
            Integer estimatedPreparationMinutes,
            String cancellationReason,
            Instant placedAt,
            LocalDateTime approvedAt,
            LocalDateTime deliveryStartedAt,
            Instant updatedAt,
            long version
    ) {
        var order = new Order(
                id,
                type,
                deliveryMode,
                status,
                deliveryAddress,
                customerInfo,
                scheduledFor,
                estimatedPreparationMinutes,
                cancellationReason,
                placedAt,
                approvedAt,
                deliveryStartedAt,
                updatedAt,
                version
        );
        order.lines.addAll(lines);
        return order;
    }

    static Result<Order> placePickUpOrder(PlacePickUpOrderCommand cmd, Clock clock) {
        var validationInput = new PlacingValidationInput(
                cmd.deliveryMode(),
                cmd.scheduledFor(),
                cmd.orderLines()
        );
        var validationResult = validateOrderPlacement(validationInput, clock);
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.getError());
        }

        var order = new Order(
                OrderType.PICKUP,
                cmd.deliveryMode(),
                cmd.orderLines(),
                null,
                cmd.customerInfo(),
                cmd.scheduledFor(),
                clock
        );

        // Emit event
        order.events.add(new PickUpOrderPlacedEvent(
                order.id,
                order.placedAt,
                List.copyOf(order.lines),
                order.deliveryAddress,
                order.customerInfo,
                order.scheduledFor
        ));

        return Result.ok(order);
    }

    static Result<Order> placeDeliveryOrder(PlaceDeliveryOrderCommand cmd, Clock clock) {
        var validationInput = new PlacingValidationInput(
                cmd.deliveryMode(),
                cmd.scheduledFor(),
                cmd.orderLines()
        );
        var validationResult = validateOrderPlacement(validationInput, clock);
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.getError());
        }

        var order = new Order(
                OrderType.DELIVERY,
                cmd.deliveryMode(),
                cmd.orderLines(),
                cmd.address(),
                cmd.customerInfo(),
                cmd.scheduledFor(),
                clock
        );

        // Emit event
        order.events.add(new DeliveryOrderPlacedEvent(
                order.id,
                order.placedAt,
                List.copyOf(order.lines),
                order.deliveryAddress,
                order.customerInfo,
                order.scheduledFor
        ));

        return Result.ok(order);
    }

    // ==== Placing Validation ====
    private record PlacingValidationInput(
            DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            List<OrderLine> lines
    ) {
    }

    private static Result<Void> validateOrderPlacement(PlacingValidationInput input, Clock clock) {
        if (input.deliveryMode() == DeliveryMode.SCHEDULED) {
            if (input.scheduledFor() == null) {
                return Result.failure("No delivery mode was specified");
            }
            if (LocalTime.now(clock).isAfter(input.scheduledFor())) {
                return Result.failure("Scheduled time must be in the future.");
            }
        }
        if (input.deliveryMode() == DeliveryMode.ASAP && input.scheduledFor() != null) {
            return Result.failure("Delivery mode must be ASAP.");
        }

        if (input.lines() == null || input.lines().isEmpty()) {
            return Result.failure("No order lines were provided");
        }

        return Result.ok(null);
    }

    // ==== Behaviour API ====
    Result<Void> approve(ApproveOrderCommand cmd, Clock clock) {
        if (status != OrderStatus.PENDING_APPROVAL) {
            return Result.failure("Only orders with status PENDING_APPROVAL can be approved by staff.");
        }
        if (deliveryMode == DeliveryMode.ASAP && (cmd.estimatedPreparationMinutes() == null || cmd.estimatedPreparationMinutes() <= 0)) {
            return Result.failure("Estimated preparation time must be provided and greater than 0 minutes for ASAP orders.");
        }

        status = OrderStatus.APPROVED;
        estimatedPreparationMinutes = cmd.estimatedPreparationMinutes();
        approvedAt = LocalDateTime.now(clock);
        updatedAt = Instant.now(clock);

        // emit event
        events.add(new OrderApprovedEvent(id, updatedAt, estimatedPreparationMinutes));

        return Result.ok(null);
    }

    Result<Void> markAsReady(Clock clock) {
        if (status != OrderStatus.APPROVED) {
            return Result.failure("Only orders with status CONFIRMED can be marked as ready.");
        }

        if (type == OrderType.PICKUP) {
            status = OrderStatus.READY_FOR_PICKUP;
        } else {
            status = OrderStatus.READY_FOR_DRIVER;
        }

        updatedAt = Instant.now(clock);

        // emit event
        events.add(new OrderMarkedAsReadyEvent(id, updatedAt, status));

        return Result.ok(null);
    }

    Result<Void> changeOrderLines(ChangeOrderLinesCommand cmd, Clock clock) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return Result.failure("Cannot change order lines to COMPLETED or CANCELLED orders.");
        }
        if (status == OrderStatus.IN_DELIVERY) {
            return Result.failure("Cannot change order lines when order is in delivery.");
        }

        var newLines = cmd.safeGetNewLines();
        var removeLines = cmd.safeGetRemovedLines();

        var cmdValidationResult = validateChangeOrderLinesInput(newLines, removeLines);
        if (cmdValidationResult.isFailure()) {
            return Result.failure(cmdValidationResult.getError());
        }

        if (!removeLines.isEmpty()) {
            var removeResult = removeLines(lines, removeLines);

            if (removeResult.isFailure()) {
                return Result.failure(removeResult.getError());
            }

            lines.clear();
            lines.addAll(removeResult.getValue());
        }

        lines.addAll(newLines);
        groupOrderLines();

        if (status == OrderStatus.READY_FOR_PICKUP ||
                status == OrderStatus.READY_FOR_DRIVER ||
                status == OrderStatus.APPROVED) {
            estimatedPreparationMinutes = cmd.updatedEstimatedPreparationTimeMinutes();
        }

        if (status == OrderStatus.READY_FOR_DRIVER || status == OrderStatus.READY_FOR_PICKUP) {
            status = OrderStatus.APPROVED;
        }

        updatedAt = Instant.now(clock);

        // emit event with snapshot of changes
        events.add(new OrderLinesChangedEvent(id, updatedAt, List.copyOf(newLines), List.copyOf(removeLines), estimatedPreparationMinutes));

        return Result.ok(null);
    }

    private static Result<Void> validateChangeOrderLinesInput(List<OrderLine> newLines, List<OrderLineRemoval> removeLines) {
        if (newLines.isEmpty() && removeLines.isEmpty()) {
            return Result.failure("No order lines to add or remove were provided.");
        }

        for (var newLine : newLines) {
            for (var removeLine : removeLines) {
                if (newLine.menuItemId().equals(removeLine.menuItemId())) {
                    return Result.failure("Cannot add and remove the same menu item in one operation.");
                }
            }
        }

        return Result.ok(null);
    }

    Result<Void> startDelivery(Clock clock) {
        if (type != OrderType.DELIVERY) {
            return Result.failure("Only DELIVERY orders can be started for delivery.");
        }
        if (status != OrderStatus.READY_FOR_DRIVER) {
            return Result.failure("Only orders with status READY_FOR_DRIVER can be started for delivery.");
        }

        status = OrderStatus.IN_DELIVERY;
        deliveryStartedAt = LocalDateTime.now(clock);
        updatedAt = Instant.now(clock);

        // emit event
        events.add(new OrderDeliveryStartedEvent(id, updatedAt));

        return Result.ok(null);
    }

    Result<Void> complete(UserPrincipal userPrincipal, Clock clock) {
        if (type == OrderType.PICKUP && status != OrderStatus.READY_FOR_PICKUP) {
            return Result.failure("Only PICKUP orders with status READY_FOR_PICKUP can be completed.");
        }
        if (type == OrderType.DELIVERY && status != OrderStatus.IN_DELIVERY) {
            return Result.failure("Only DELIVERY orders with status IN_DELIVERY can be completed.");
        }

        if (type != OrderType.DELIVERY &&
                userPrincipal.roles().contains(UserPrincipal.Role.DRIVER)
        ) {
            return Result.failure("User with DRIVER role cannot complete non-DELIVERY orders.");
        }

        status = OrderStatus.COMPLETED;
        updatedAt = Instant.now(clock);

        // emit event
        events.add(new OrderCompletedEvent(id, updatedAt));

        return Result.ok(null);
    }

    Result<Void> cancel(CancelOrderCommand cmd, Clock clock) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return Result.failure("Cannot cancel a completed or already cancelled order.");
        }

        status = OrderStatus.CANCELLED;
        cancellationReason = cmd.reason();

        updatedAt = Instant.now(clock);

        // emit event
        events.add(new OrderCancelledEvent(id, updatedAt, cancellationReason));

        return Result.ok(null);
    }

    boolean isFinished() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
    }

    // ==== Helper methods ====
    private static Result<List<OrderLine>> removeLines(List<OrderLine> currentLines, List<OrderLineRemoval> orderLineRemovals) {
        var groupedCurrentLines = currentLines.stream()
                .collect(Collectors.toMap(OrderLine::menuItemId, x -> x));

        for (var removeLine : orderLineRemovals) {
            var existingLine = groupedCurrentLines.get(removeLine.menuItemId());
            if (existingLine == null) {
                return Result.failure("Cannot remove a non-existing order line: " + removeLine.menuItemId());
            }
            if (removeLine.quantity() > existingLine.quantity()) {
                return Result.failure("Cannot remove more quantity than exists for menu item id: " + removeLine.menuItemId());
            }

            var remainingQuantity = existingLine.quantity() - removeLine.quantity();
            if (remainingQuantity > 0) {
                var updatedLine = new OrderLine(
                        existingLine.menuItemId(),
                        remainingQuantity,
                        existingLine.unitPrice(),
                        existingLine.menuItemName()
                );
                groupedCurrentLines.put(removeLine.menuItemId(), updatedLine);
            } else {
                groupedCurrentLines.remove(removeLine.menuItemId());
            }
        }

        return Result.ok(new ArrayList<>(groupedCurrentLines.values()));
    }

    private void groupOrderLines() {
        var groupedMap = new HashMap<String, OrderLine>();
        for (var line : lines) {
            if (groupedMap.containsKey(line.menuItemId())) {
                var existingLine = groupedMap.get(line.menuItemId());
                var updatedQuantity = existingLine.quantity() + line.quantity();
                var updatedLine = new OrderLine(
                        line.menuItemId(),
                        updatedQuantity,
                        line.unitPrice(),
                        line.menuItemName()
                );
                groupedMap.put(line.menuItemId(), updatedLine);
            } else {
                groupedMap.put(line.menuItemId(), line);
            }
        }

        lines.clear();
        lines.addAll(groupedMap.values());
    }

    // ==== Getters for Collections ====
    List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    List<OrderEvent> pullEvents() {
        var emittedEvents = List.copyOf(events);
        events.clear();
        return emittedEvents;
    }

    // ==== Identity ====
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
