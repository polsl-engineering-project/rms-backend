package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.order.cmd.*;
import com.polsl.engineering.project.rms.order.vo.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Table(name = "orders")
@Entity(name = "Order")
class Order {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id"))
    private OrderId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode")
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus status;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLine> lines = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "delivery_street"))
    @AttributeOverride(name = "houseNumber", column = @Column(name = "delivery_house_number"))
    @AttributeOverride(name = "apartmentNumber", column = @Column(name = "delivery_apartment_number"))
    @AttributeOverride(name = "city", column = @Column(name = "delivery_city"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "delivery_postal_code"))
    private Address deliveryAddress;

    @Embedded
    @AttributeOverride(name = "firstName", column = @Column(name = "customer_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "customer_last_name"))
    @AttributeOverride(name = "phoneNumber", column = @Column(name = "customer_phone_number"))
    private CustomerInfo customerInfo;

    @Column(name = "scheduled_for")
    private LocalTime scheduledFor;

    @Column(name = "estimated_preparation_minutes")
    private Integer estimatedPreparationMinutes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // ==== Constructors ====
    protected Order() {
        // For JPA
    }

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
    }

    // ==== Factory Methods ====
    public static Result<Order> placePickUpOrder(PlacePickUpOrderCommand cmd, Clock clock) {
        var validationInput = new PlacingValidationInput(
                cmd.deliveryMode(),
                cmd.scheduledFor(),
                cmd.orderLines()
        );
        var validationResult = validateOrderPlacement(validationInput, clock);
        if (validationResult.isFailure) {
            return Result.failure(validationResult.failureReason);
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

        return Result.ok(order);
    }

    public static Result<Order> placeDeliveryOrder(PlaceDeliveryOrderCommand cmd, Clock clock) {
        var validationInput = new PlacingValidationInput(
                cmd.deliveryMode(),
                cmd.scheduledFor(),
                cmd.orderLines()
        );
        var validationResult = validateOrderPlacement(validationInput, clock);
        if (validationResult.isFailure) {
            return Result.failure(validationResult.failureReason);
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

        return Result.ok(order);
    }

    // ==== Placing Validation ====
    private record PlacingValidationResult(boolean isFailure, String failureReason) {
        static PlacingValidationResult success() {
            return new PlacingValidationResult(false, null);
        }

        static PlacingValidationResult failure(String failureReason) {
            return new PlacingValidationResult(true, failureReason);
        }
    }

    private record PlacingValidationInput(
            DeliveryMode deliveryMode,
            LocalTime scheduledFor,
            List<OrderLine> lines
    ) {
    }

    private static PlacingValidationResult validateOrderPlacement(PlacingValidationInput input, Clock clock) {
        if (input.deliveryMode() == DeliveryMode.SCHEDULED) {
            if (input.scheduledFor() == null) {
                return PlacingValidationResult.failure("No delivery mode was specified");
            }
            if (LocalTime.now(clock).isAfter(input.scheduledFor())) {
                return PlacingValidationResult.failure("Scheduled time must be in the future.");
            }
        }
        if (input.deliveryMode() == DeliveryMode.ASAP && input.scheduledFor() != null) {
            return PlacingValidationResult.failure("Delivery mode must be ASAP.");
        }

        if (input.lines() == null || input.lines().isEmpty()) {
            return PlacingValidationResult.failure("No order lines were provided");
        }

        return PlacingValidationResult.success();
    }

    // ==== Behaviour API ====
    Result<Void> approveByFrontDesk(Clock clock) {
        if (status != OrderStatus.PENDING_APPROVAL) {
            return Result.failure("Only orders with status PENDING_APPROVAL can be approved by staff.");
        }

        status = OrderStatus.APPROVED_BY_FRONT_DESK;
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    Result<Void> approveByKitchen(ApproveOrderByKitchenCommand cmd, Clock clock) {
        if (status != OrderStatus.APPROVED_BY_FRONT_DESK) {
            return Result.failure("Only orders with status APPROVED_BY_FRONT_DESK can be approved by kitchen.");
        }
        if (deliveryMode == DeliveryMode.ASAP && (cmd.estimatedPreparationMinutes() == null || cmd.estimatedPreparationMinutes() <= 0)) {
            return Result.failure("Estimated preparation time must be provided and greater than 0 minutes for ASAP orders.");
        }

        status = OrderStatus.CONFIRMED;
        estimatedPreparationMinutes = cmd.estimatedPreparationMinutes();

        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    public Result<Void> markAsReady(Clock clock) {
        if (status != OrderStatus.CONFIRMED) {
            return Result.failure("Only orders with status CONFIRMED can be marked as ready.");
        }

        if (type == OrderType.PICKUP) {
            status = OrderStatus.READY_FOR_PICKUP;
        } else {
            status = OrderStatus.READY_FOR_DRIVER;
        }

        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    public Result<Void> addItemsByStaff(AddItemsByStaffCommand cmd, Clock clock) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return Result.failure("Cannot add items to a completed or cancelled order.");
        }

        if (cmd.newLines() == null || cmd.newLines().isEmpty()) {
            return Result.failure("No new order lines were provided.");
        }

        if (deliveryMode == DeliveryMode.ASAP &&
                status.ordinal() >= OrderStatus.CONFIRMED.ordinal() &&
                cmd.updatedEstimatedMinutes() == null
        ) {
            return Result.failure("Updated estimated preparation time must be provided for ASAP orders.");
        }

        this.lines.addAll(cmd.newLines());

        if (cmd.updatedEstimatedMinutes() != null) {
            this.estimatedPreparationMinutes = cmd.updatedEstimatedMinutes();
        }

        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    public Result<Void> startDelivery(Clock clock) {
        if (type != OrderType.DELIVERY) {
            return Result.failure("Only DELIVERY orders can be started for delivery.");
        }
        if (status != OrderStatus.READY_FOR_DRIVER) {
            return Result.failure("Only orders with status READY_FOR_DRIVER can be started for delivery.");
        }

        status = OrderStatus.IN_DELIVERY;
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    public Result<Void> complete(Clock clock) {
        if (type == OrderType.PICKUP && status != OrderStatus.READY_FOR_PICKUP) {
            return Result.failure("Only PICKUP orders with status READY_FOR_PICKUP can be completed.");
        }
        if (type == OrderType.DELIVERY && status != OrderStatus.IN_DELIVERY) {
            return Result.failure("Only DELIVERY orders with status IN_DELIVERY can be completed.");
        }

        status = OrderStatus.COMPLETED;
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    public Result<Void> cancel(CancelOrderCommand cmd, Clock clock) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return Result.failure("Cannot cancel a completed or already cancelled order.");
        }

        status = OrderStatus.CANCELLED;
        cancellationReason = cmd.reason();

        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    // ==== Getters for Collections ====
    List<OrderLine> getLines() {
        return List.copyOf(lines);
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
