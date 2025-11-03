package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.AddItemsByStaffCommand;
import com.polsl.engineering.project.rms.order.cmd.ApproveOrderByKitchenCommand;
import com.polsl.engineering.project.rms.order.cmd.CancelOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlaceDeliveryOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlacePickUpOrderCommand;
import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order aggregate unit tests")
class OrderTest {

    // helpers
    private static Money m(String amount) {
        return new Money(new BigDecimal(amount));
    }

    private static OrderLine line(String menuItemId, int qty, String unitPrice) {
        return new OrderLine(menuItemId, qty, m(unitPrice));
    }

    private static CustomerInfo customer() {
        return new CustomerInfo("John", "Doe", "+48123123123");
    }

    private static Address address() {
        return new Address("Main St", "1", "2", "City", "00-001");
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("GivenValidPickupOrder_WhenPlacePickUpOrder_ThenSuccessAndFieldsSet")
    void GivenValidPickupOrder_WhenPlacePickUpOrder_ThenSuccessAndFieldsSet() {
        // given
        var cmd = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("item-1", 2, "10.00")));

        // when
        var res = Order.placePickUpOrder(cmd, fixedClock());

        // then
        assertThat(res).isNotNull();
        assertThat(res.isSuccess()).isTrue();
        var order = res.getValue().orElseThrow();
        assertThat(order.getType()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderType.PICKUP);
        assertThat(order.getDeliveryMode()).isEqualTo(DeliveryMode.ASAP);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getCustomerInfo()).isEqualTo(customer());
        assertThat(order.getPlacedAt()).isEqualTo(Instant.now(fixedClock()));
    }

    @Test
    @DisplayName("GivenScheduledDeliveryWithoutTime_WhenPlaceOrder_ThenFailure")
    void GivenScheduledDeliveryWithoutTime_WhenPlaceOrder_ThenFailure() {
        // given
        var cmd = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.SCHEDULED, null, List.of(line("i",1,"1.00")));

        // when
        var res = Order.placeDeliveryOrder(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("No delivery mode was specified"));
    }

    @Test
    @DisplayName("GivenScheduledDeliveryWithPastTime_WhenPlaceOrder_ThenFailure")
    void GivenScheduledDeliveryWithPastTime_WhenPlaceOrder_ThenFailure() {
        // given (fixedClock is 12:00 UTC -> 11:00 is in the past)
        var cmd = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.SCHEDULED, LocalTime.of(11,0), List.of(line("i",1,"1.00")));

        // when
        var res = Order.placeDeliveryOrder(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Scheduled time must be in the future."));
    }

    @Test
    @DisplayName("GivenScheduledDeliveryWithFutureTime_WhenPlaceOrder_ThenSuccess")
    void GivenScheduledDeliveryWithFutureTime_WhenPlaceOrder_ThenSuccess() {
        // given (fixedClock is 12:00 UTC -> 13:00 is in the future)
        var cmd = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.SCHEDULED, LocalTime.of(13,0), List.of(line("i",1,"1.00")));

        // when
        var res = Order.placeDeliveryOrder(cmd, fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("GivenAsapWithScheduledTime_WhenPlaceOrder_ThenFailure")
    void GivenAsapWithScheduledTime_WhenPlaceOrder_ThenFailure() {
        // given
        var cmd = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, LocalTime.of(13,0), List.of(line("i",1,"1.00")));

        // when
        var res = Order.placePickUpOrder(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Delivery mode must be ASAP."));
    }

    @Test
    @DisplayName("GivenEmptyOrderLines_WhenPlaceOrder_ThenFailure")
    void GivenEmptyOrderLines_WhenPlaceOrder_ThenFailure() {
        // given
        var cmd = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of());

        // when
        var res = Order.placePickUpOrder(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("No order lines were provided"));
    }

    @Test
    @DisplayName("GivenOrderPending_WhenApproveByFrontDesk_ThenStatusChanged")
    void GivenOrderPending_WhenApproveByFrontDesk_ThenStatusChanged() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var res = order.approveByFrontDesk(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.APPROVED_BY_FRONT_DESK);
    }

    @Test
    @DisplayName("GivenOrderNotPending_WhenApproveByFrontDesk_ThenFailure")
    void GivenOrderNotPending_WhenApproveByFrontDesk_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());

        // when
        var res = order.approveByFrontDesk(fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Only orders with status PENDING_APPROVAL can be approved by staff."));
    }

    @Test
    @DisplayName("GivenNotApprovedByFrontDesk_WhenApproveByKitchen_ThenFailure")
    void GivenNotApprovedByFrontDesk_WhenApproveByKitchen_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var cmd = new ApproveOrderByKitchenCommand(5);
        var res = order.approveByKitchen(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Only orders with status APPROVED_BY_FRONT_DESK can be approved by kitchen."));
    }

    @Test
    @DisplayName("GivenApprovedByFrontDesk_WhenApproveByKitchen_AsapMissingMinutes_ThenFailure")
    void GivenApprovedByFrontDesk_WhenApproveByKitchen_AsapMissingMinutes_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());

        // when
        var cmd = new ApproveOrderByKitchenCommand(null);
        var res = order.approveByKitchen(cmd, fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Estimated preparation time must be provided"));
    }

    @Test
    @DisplayName("GivenApprovedByFrontDesk_WhenApproveByKitchen_WithMinutes_ThenConfirmedAndEstimatedSet")
    void GivenApprovedByFrontDesk_WhenApproveByKitchen_WithMinutes_ThenConfirmedAndEstimatedSet() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());

        // when
        var cmd = new ApproveOrderByKitchenCommand(15);
        var res = order.approveByKitchen(cmd, fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.CONFIRMED);
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("GivenConfirmedPickup_WhenMarkAsReady_ThenReadyForPickup")
    void GivenConfirmedPickup_WhenMarkAsReady_ThenReadyForPickup() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(5), fixedClock());

        // when
        var res = order.markAsReady(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.READY_FOR_PICKUP);
    }

    @Test
    @DisplayName("GivenConfirmedDelivery_WhenMarkAsReady_ThenReadyForDriver")
    void GivenConfirmedDelivery_WhenMarkAsReady_ThenReadyForDriver() {
        // given
        var place = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placeDeliveryOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock());

        // when
        var res = order.markAsReady(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.READY_FOR_DRIVER);
    }

    @Test
    @DisplayName("GivenReadyForDriverDelivery_WhenStartDelivery_ThenInDelivery")
    void GivenReadyForDriverDelivery_WhenStartDelivery_ThenInDelivery() {
        // given
        var place = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placeDeliveryOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock());
        order.markAsReady(fixedClock());

        // when
        var res = order.startDelivery(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.IN_DELIVERY);
    }

    @Test
    @DisplayName("GivenPickupOrder_WhenStartDelivery_ThenFailure")
    void GivenPickupOrder_WhenStartDelivery_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var res = order.startDelivery(fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Only DELIVERY orders can be started for delivery."));
    }

    @Test
    @DisplayName("GivenInDelivery_WhenCompleteDelivery_ThenCompleted")
    void GivenInDelivery_WhenCompleteDelivery_ThenCompleted() {
        // given
        var place = new PlaceDeliveryOrderCommand(customer(), address(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placeDeliveryOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock());
        order.markAsReady(fixedClock());
        order.startDelivery(fixedClock());

        // when
        var res = order.complete(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("GivenReadyForPickup_WhenCompletePickup_ThenCompleted")
    void GivenReadyForPickup_WhenCompletePickup_ThenCompleted() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock());
        order.markAsReady(fixedClock());

        // when
        var res = order.complete(fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("GivenCompleted_WhenCancel_ThenFailure")
    void GivenCompleted_WhenCancel_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock());
        order.markAsReady(fixedClock());
        order.complete(fixedClock());

        // when
        var res = order.cancel(new CancelOrderCommand("no"), fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Cannot cancel a completed or already cancelled order."));
    }

    @Test
    @DisplayName("GivenPending_WhenCancel_ThenCancelledAndReasonSet")
    void GivenPending_WhenCancel_ThenCancelledAndReasonSet() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var res = order.cancel(new CancelOrderCommand("customer changed mind"), fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(com.polsl.engineering.project.rms.order.vo.OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("customer changed mind");
    }

    @Test
    @DisplayName("GivenOrderActive_WhenAddItemsByStaff_NoNewLines_ThenFailure")
    void GivenOrderActive_WhenAddItemsByStaff_NoNewLines_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var res = order.addItemsByStaff(new AddItemsByStaffCommand(List.of(), null), fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("No new order lines were provided."));
    }

    @Test
    @DisplayName("GivenOrderCompleted_WhenAddItemsByStaff_ThenFailure")
    void GivenOrderCompleted_WhenAddItemsByStaff_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(5), fixedClock());
        order.markAsReady(fixedClock());
        order.complete(fixedClock());

        // when
        var res = order.addItemsByStaff(new AddItemsByStaffCommand(List.of(line("b",1,"1")), null), fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Cannot add items to a completed or cancelled order."));
    }

    @Test
    @DisplayName("GivenOrderActive_WhenAddItemsByStaff_AsapNeedsUpdatedMinutes_ThenFailure")
    void GivenOrderActive_WhenAddItemsByStaff_AsapNeedsUpdatedMinutes_ThenFailure() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();
        order.approveByFrontDesk(fixedClock());
        order.approveByKitchen(new ApproveOrderByKitchenCommand(10), fixedClock()); // CONFIRMED

        // when
        var res = order.addItemsByStaff(new AddItemsByStaffCommand(List.of(line("b",1,"1")), null), fixedClock());

        // then
        assertThat(res.isFailure()).isTrue();
        assertThat(res.getError()).hasValueSatisfying(s -> assertThat(s).contains("Updated estimated preparation time must be provided for ASAP orders."));
    }

    @Test
    @DisplayName("GivenOrderActive_WhenAddItemsByStaff_SuccessAndLinesAdded")
    void GivenOrderActive_WhenAddItemsByStaff_SuccessAndLinesAdded() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        // when
        var res = order.addItemsByStaff(new AddItemsByStaffCommand(List.of(line("b",2,"2.00")), null), fixedClock());

        // then
        assertThat(res.isSuccess()).isTrue();
        assertThat(order.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("GivenOrder_WhenGetLines_ThenReturnsUnmodifiableCopy")
    void GivenOrder_WhenGetLines_ThenReturnsUnmodifiableCopy() {
        // given
        var place = new PlacePickUpOrderCommand(customer(), DeliveryMode.ASAP, null, List.of(line("a",1,"1")));
        var order = Order.placePickUpOrder(place, fixedClock()).getValue().orElseThrow();

        var lines = order.getLines();
        var newLine = line("x",1,"1");
        // attempting to modify should throw
        assertThatThrownBy(() -> lines.add(newLine))
                .isInstanceOf(UnsupportedOperationException.class);

        // original size unchanged
        assertThat(order.getLines()).hasSize(1);
    }

}
