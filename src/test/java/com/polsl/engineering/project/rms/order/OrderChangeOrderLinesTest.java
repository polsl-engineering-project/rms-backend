package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.ApproveOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.ChangeOrderLinesCommand;
import com.polsl.engineering.project.rms.order.cmd.PlaceDeliveryOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlacePickUpOrderCommand;
import com.polsl.engineering.project.rms.order.vo.*;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order.changeOrderLines")
class OrderChangeOrderLinesTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    // helpers
    private static Order createPickupOrderConfirmed(List<OrderLine> initialLines) {
        var cmd = new PlacePickUpOrderCommand(
                new CustomerInfo("John", "Doe", "123456789"),
                DeliveryMode.SCHEDULED,
                LocalTime.of(13, 0),
                initialLines
        );
        var placed = Order.placePickUpOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        var order = placed.getValue();
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        return order;
    }

    private static Order createDeliveryOrderConfirmed(List<OrderLine> initialLines) {
        var cmd = new PlaceDeliveryOrderCommand(
                new CustomerInfo("Jane", "Doe", "987654321"),
                new Address("Main", "1", null, "City", "00-000"),
                DeliveryMode.SCHEDULED,
                LocalTime.of(13, 0),
                initialLines
        );
        var placed = Order.placeDeliveryOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        var order = placed.getValue();
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        return order;
    }

    private static OrderLine line(String id, int qty, String price, long version) {
        return new OrderLine(id, qty, new Money(new BigDecimal(price)), version);
    }

    private static OrderLineRemoval lineRemoval(String id, int qty) {
        return new OrderLineRemoval(id, qty);
    }

    // helper user principals
    private static UserPrincipal nonDriverUser() {
        return new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));
    }

    private static UserPrincipal driverUser() {
        return new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.DRIVER));
    }

    @Test
    @DisplayName("Given confirmed order with items, When adding only new lines, Then lines added and status unchanged")
    void GivenConfirmedOrder_WhenAddOnlyNewLines_ThenLinesAddedAndStatusUnchanged() {
        // given
        var order = createPickupOrderConfirmed(List.of(
                line("pizza", 1, "30.00", 1)
        ));
        var newLines = List.of(
                line("pasta", 2, "25.50", 3),
                line("salad", 1, "12.00", 2)
        );
        var cmd = new ChangeOrderLinesCommand(newLines, null, 10);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(10);
        assertThat(order.getLines())
                .extracting(OrderLine::menuItemId, OrderLine::quantity, OrderLine::menuItemVersion)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("pizza", 1, 1L),
                        org.assertj.core.groups.Tuple.tuple("pasta", 2, 3L),
                        org.assertj.core.groups.Tuple.tuple("salad", 1, 2L)
                );
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("Given any order, When add and remove the same menu item, Then failure")
    void GivenAnyOrder_WhenAddAndRemoveSameMenuItemInOneOperation_ThenFailure() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("soup", 1, "10.00", 1)));
        var newLines = List.of(line("soup", 1, "10.00", 2));
        var removeLines = List.of(lineRemoval("soup", 1));
        var cmd = new ChangeOrderLinesCommand(newLines, removeLines, 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cannot add and remove the same menu item in one operation.");
    }

    @Test
    @DisplayName("Given order COMPLETED, When changeOrderLines, Then failure")
    void GivenOrderCompleted_WhenChangeOrderLines_ThenFailure() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("cake", 1, "15.00", 1)));
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(order.complete(nonDriverUser(), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        var cmd = new ChangeOrderLinesCommand(List.of(line("tea", 1, "5.00", 1)), List.of(), 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cannot change order lines to COMPLETED or CANCELLED orders.");
    }

    @Test
    @DisplayName("Given order IN_DELIVERY, When changeOrderLines, Then failure")
    void GivenOrderInDelivery_WhenChangeOrderLines_ThenFailure() {
        // given
        var order = createDeliveryOrderConfirmed(List.of(line("pizza", 1, "30.00", 1)));
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DRIVER);
        assertThat(order.startDelivery(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_DELIVERY);

        var cmd = new ChangeOrderLinesCommand(List.of(line("drink", 2, "6.00", 1)), List.of(), 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cannot change order lines when order is in delivery.");
    }

    @Test
    @DisplayName("Given order CANCELLED, When changeOrderLines, Then failure")
    void GivenOrderCancelled_WhenChangeOrderLines_ThenFailure() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("soda", 1, "5.00", 1)));
        assertThat(order.cancel(new com.polsl.engineering.project.rms.order.cmd.CancelOrderCommand("client request"), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        var cmd = new ChangeOrderLinesCommand(List.of(line("water", 1, "3.00", 1)), List.of(), 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cannot change order lines to COMPLETED or CANCELLED orders.");
    }

    @Test
    @DisplayName("Given order READY_FOR_PICKUP, When changeOrderLines, Then status reverted to CONFIRMED")
    void GivenReadyForPickupOrder_WhenChangeOrderLines_ThenStatusRevertedToConfirmed() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("wrap", 1, "18.00", 1)));
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);

        var cmd = new ChangeOrderLinesCommand(List.of(line("cookie", 1, "4.00", 1)), List.of(), 15);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("Given order READY_FOR_DRIVER, When changeOrderLines, Then status reverted to CONFIRMED")
    void GivenReadyForDriverOrder_WhenChangeOrderLines_ThenStatusRevertedToConfirmed() {
        // given
        var order = createDeliveryOrderConfirmed(List.of(line("wrap", 1, "18.00", 1)));
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DRIVER);

        var cmd = new ChangeOrderLinesCommand(List.of(line("cookie", 1, "4.00", 1)), List.of(), 17);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(17);
    }

    @Test
    @DisplayName("Given null lists, When changeOrderLines, Then failure - no lines provided")
    void GivenNullLists_WhenChangeOrderLines_ThenFailureNoLinesProvided() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("wrap", 1, "18.00", 1)));

        var cmd = new ChangeOrderLinesCommand(null, null, 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("No order lines to add or remove were provided.");
    }

    @Test
    @DisplayName("Given empty lists, When changeOrderLines, Then failure - no lines provided")
    void GivenEmptyLists_WhenChangeOrderLines_ThenFailureNoLinesProvided() {
        // given
        var order = createPickupOrderConfirmed(List.of(line("wrap", 1, "18.00", 1)));

        var cmd = new ChangeOrderLinesCommand(List.of(), List.of(), 0);

        // when
        var result = order.changeOrderLines(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("No order lines to add or remove were provided.");
    }

    @Test
    @DisplayName("Given driver role, When attempting to complete non-delivery order, Then failure")
    void GivenDriverRole_WhenCompleteNonDelivery_ThenFailure() {
        // given: pickup order
        var order = createPickupOrderConfirmed(List.of(line("sandwich", 1, "8.00", 1)));
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);

        // when: driver attempts to complete
        var completeResult = order.complete(driverUser(), FIXED_CLOCK);

        // then
        assertThat(completeResult.isFailure()).isTrue();
        assertThat(completeResult.getError()).isEqualTo("User with DRIVER role cannot complete non-DELIVERY orders.");
    }

}
