package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.ApproveOrderCommand;
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

@DisplayName("Order - other methods")
class OrderOtherMethodsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    private static OrderLine line(String id, String price) {
        return new OrderLine(id, 1, new Money(new BigDecimal(price)), id);
    }

    private static Order placePickup(List<OrderLine> initialLines) {
        var cmd = new PlacePickUpOrderCommand(
                new CustomerInfo("John", "Doe", "123456789"),
                DeliveryMode.SCHEDULED,
                LocalTime.of(13, 0),
                initialLines
        );
        var placed = Order.placePickUpOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        return placed.getValue();
    }

    private static Order placeDelivery(List<OrderLine> initialLines) {
        var cmd = new PlaceDeliveryOrderCommand(
                new CustomerInfo("Jane", "Doe", "987654321"),
                new Address("Main", "1", null, "City", "00-000"),
                DeliveryMode.SCHEDULED,
                LocalTime.of(13, 0),
                initialLines
        );
        var placed = Order.placeDeliveryOrder(cmd, FIXED_CLOCK);
        assertThat(placed.isSuccess()).isTrue();
        return placed.getValue();
    }

    // helper user principals
    private static UserPrincipal nonDriverUser() {
        return new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));
    }

    private static UserPrincipal driverUser() {
        return new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.DRIVER));
    }

    @Test
    @DisplayName("Given pickup order - when approve flow - then status CONFIRMED")
    void GivenPickupOrder_WhenApproveFlow_ThenConfirmed() {
        // given
        var order = placePickup(List.of(line("pizza", "30.00")));

        // when
        var result = order.approve(new ApproveOrderCommand(null), FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("Given delivery order - when approve flow - then status CONFIRMED")
    void GivenDeliveryOrder_WhenApproveFlow_ThenConfirmed() {
        // given
        var order = placeDelivery(List.of(line("pizza", "30.00")));

        // when
        var result = order.approve(new ApproveOrderCommand(null), FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("Given pickup order - when mark as ready and complete - then READY_FOR_PICKUP and COMPLETED")
    void GivenPickupOrder_WhenMarkReadyAndComplete_ThenReadyAndCompleted() {
        // given
        var order = placePickup(List.of(line("cake", "15.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();

        // when
        var markReadyResult = order.markAsReady(FIXED_CLOCK);

        // then
        assertThat(markReadyResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);

        // when
        var completeResult = order.complete(nonDriverUser(), FIXED_CLOCK);

        // then
        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Given delivery order - when mark as ready and start delivery - then READY_FOR_DRIVER and IN_DELIVERY")
    void GivenDeliveryOrder_WhenMarkReadyAndStartDelivery_ThenReadyAndInDelivery() {
        // given
        var order = placeDelivery(List.of(line("wrap", "18.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();

        // when
        var markReadyResult = order.markAsReady(FIXED_CLOCK);

        // then
        assertThat(markReadyResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DRIVER);

        // when
        var startDeliveryResult = order.startDelivery(FIXED_CLOCK);

        // then
        assertThat(startDeliveryResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_DELIVERY);
    }

    @Test
    @DisplayName("Given confirmed order - when cancel - then CANCELLED")
    void GivenConfirmedOrder_WhenCancel_ThenCancelled() {
        // given
        var order = placePickup(List.of(line("soda", "5.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();

        // when
        var cancelResult = order.cancel(new com.polsl.engineering.project.rms.order.cmd.CancelOrderCommand("client request"), FIXED_CLOCK);

        // then
        assertThat(cancelResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("GivenNewOrder_WhenNotFinished_ThenIsFinishedFalse")
    void GivenNewOrder_WhenNotFinished_ThenIsFinishedFalse() {
        //given
        var order = placePickup(List.of(line("burger", "12.00")));

        //when
        var finished = order.isFinished();

        //then
        assertThat(finished).isFalse();
    }

    @Test
    @DisplayName("GivenCompletedOrder_WhenComplete_ThenIsFinishedTrue")
    void GivenCompletedOrder_WhenComplete_ThenIsFinishedTrue() {
        //given
        var order = placePickup(List.of(line("cake", "15.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();

        //when
        var completeResult = order.complete(nonDriverUser(), FIXED_CLOCK);

        //then
        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(order.isFinished()).isTrue();
    }

    @Test
    @DisplayName("GivenConfirmedOrder_WhenCancel_ThenIsFinishedTrue")
    void GivenConfirmedOrder_WhenCancel_ThenIsFinishedTrue() {
        //given
        var order = placePickup(List.of(line("soda", "5.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();

        //when
        var cancelResult = order.cancel(new com.polsl.engineering.project.rms.order.cmd.CancelOrderCommand("client request"), FIXED_CLOCK);

        //then
        assertThat(cancelResult.isSuccess()).isTrue();
        assertThat(order.isFinished()).isTrue();
    }

    @Test
    @DisplayName("Given delivery order, When driver completes in delivery, Then COMPLETED")
    void GivenDeliveryOrder_WhenDriverCompletes_ThenCompleted() {
        // given
        var order = placeDelivery(List.of(line("pizza", "30.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.startDelivery(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_DELIVERY);

        // when
        var completeResult = order.complete(driverUser(), FIXED_CLOCK);

        // then
        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Given delivery order ready for driver, When driver attempts to complete early, Then failure")
    void GivenDeliveryOrderReadyForDriver_WhenDriverAttemptsCompleteEarly_ThenFailure() {
        // given
        var order = placeDelivery(List.of(line("pasta", "20.00")));
        assertThat(order.approve(new ApproveOrderCommand(null), FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.markAsReady(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DRIVER);

        // when
        var completeResult = order.complete(driverUser(), FIXED_CLOCK);

        // then
        assertThat(completeResult.isFailure()).isTrue();
        assertThat(completeResult.getError()).isEqualTo("Only DELIVERY orders with status IN_DELIVERY can be completed.");
    }
}
