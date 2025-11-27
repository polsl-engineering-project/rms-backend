package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.ApproveOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlaceDeliveryOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlacePickUpOrderCommand;
import com.polsl.engineering.project.rms.order.vo.*;
import com.polsl.engineering.project.rms.order.event.OrderApprovedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order - approve method")
class OrderApproveTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    private static OrderLine line(String id, int qty, String price) {
        return new OrderLine(id, qty, new Money(new BigDecimal(price)), id);
    }

    private static Order placePickup(List<OrderLine> initialLines, DeliveryMode deliveryMode, LocalTime scheduledFor) {
        var cmd = new PlacePickUpOrderCommand(
                new CustomerInfo("John", "Doe", "123456789"),
                deliveryMode,
                scheduledFor,
                initialLines
        );
        var placed = Order.placePickUpOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        var order = placed.getValue();
        // clear the place event emitted during placement so tests can assert only approve-related events
        order.pullEvents();
        return order;
    }

    private static Order placeDelivery(List<OrderLine> initialLines) {
        var cmd = new PlaceDeliveryOrderCommand(
                new CustomerInfo("Jane", "Doe", "987654321"),
                new Address("Main", "1", null, "City", "00-000"),
                DeliveryMode.ASAP,
                null,
                initialLines
        );
        var placed = Order.placeDeliveryOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        var order = placed.getValue();
        // clear the place event emitted during placement so tests can assert only approve-related events
        order.pullEvents();
        return order;
    }

    @Test
    @DisplayName("Given scheduled pickup order - when approve with null estimate - then success and CONFIRMED")
    void givenScheduledPickup_whenApproveWithNullEstimate_thenConfirmed() {
        var order = placePickup(List.of(line("pizza", 1, "30.00")), DeliveryMode.SCHEDULED, LocalTime.of(13, 0));

        var result = order.approve(new ApproveOrderCommand(null), FIXED_CLOCK);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getEstimatedPreparationMinutes()).isNull();

        var events = order.pullEvents();
        assertThat(events).hasSize(1);
        var evt = events.getFirst();
        assertThat(evt).isInstanceOf(OrderApprovedEvent.class);
        var approved = (OrderApprovedEvent) evt;
        assertThat(approved.estimatedPreparationMinutes()).isNull();
        // use OrderEvent.getOccurredAt() for Instant (occurrence time)
        assertThat(evt.getOccurredAt()).isEqualTo(Instant.now(FIXED_CLOCK));
    }

    @Test
    @DisplayName("Given ASAP delivery order - when approve with positive estimate - then success and CONFIRMED")
    void givenAsapDelivery_whenApproveWithPositiveEstimate_thenConfirmed() {
        var order = placeDelivery(List.of(line("sushi", 2, "20.00")));

        var result = order.approve(new ApproveOrderCommand(15), FIXED_CLOCK);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(15);

        var events = order.pullEvents();
        assertThat(events).hasSize(1);
        var evt = events.getFirst();
        assertThat(evt).isInstanceOf(OrderApprovedEvent.class);
        var approved = (OrderApprovedEvent) evt;
        assertThat(approved.estimatedPreparationMinutes()).isEqualTo(15);
        // use OrderEvent.getOccurredAt() for Instant (occurrence time)
        assertThat(evt.getOccurredAt()).isEqualTo(Instant.now(FIXED_CLOCK));
    }

    @Test
    @DisplayName("Given ASAP order - when approve with null estimate - then failure")
    void givenAsapOrder_whenApproveWithNullEstimate_thenFailure() {
        var order = placePickup(List.of(line("salad", 1, "10.00")), DeliveryMode.ASAP, null);

        var result = order.approve(new ApproveOrderCommand(null), FIXED_CLOCK);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Estimated preparation time must be provided and greater than 0 minutes for ASAP orders.");
    }

    @Test
    @DisplayName("Given ASAP order - when approve with non-positive estimate - then failure")
    void givenAsapOrder_whenApproveWithZeroEstimate_thenFailure() {
        var order = placeDelivery(List.of(line("soup", 1, "8.00")));

        var result = order.approve(new ApproveOrderCommand(0), FIXED_CLOCK);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Estimated preparation time must be provided and greater than 0 minutes for ASAP orders.");
    }

}
