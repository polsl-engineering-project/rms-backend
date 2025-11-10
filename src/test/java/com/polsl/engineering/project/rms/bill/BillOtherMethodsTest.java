package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.cmd.PayBillCommand;
import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.order.vo.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bill - other methods (open, close, pay)")
class BillOtherMethodsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    private static BillLine line(String id, int qty, String price, String name, long version) {
        return new BillLine(id, qty, new Money(new BigDecimal(price)), name, version);
    }


    @Test
    @DisplayName("Given valid command, When open bill, Then bill created with OPEN status")
    void GivenValidCommand_WhenOpenBill_ThenBillCreatedWithOpenStatus() {
        // given
        var tableNumber = TableNumber.of(7);
        var pizzaId = UUID.randomUUID().toString();
        var saladId = UUID.randomUUID().toString();
        var waiterInfo = new WaiterInfo("Jane", "Smith", UUID.randomUUID().toString());
        var lines = List.of(
                line(pizzaId, 2, "30.00", "Pizza", 1),
                line(saladId, 1, "12.00", "Salad", 1)
        );
        var cmd = new OpenBillCommand(tableNumber, waiterInfo, lines);

        // when
        var result = Bill.open(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        var bill = result.getValue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.OPEN);
        assertThat(bill.getTableNumber()).isEqualTo(tableNumber);
        assertThat(bill.getWaiterInfo()).isEqualTo(waiterInfo);
        assertThat(bill.getLines()).hasSize(2);
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("72.00"));
        assertThat(bill.getPaidAmount()).isEqualTo(Money.ZERO);
        assertThat(bill.getOpenedAt()).isNotNull();
        assertThat(bill.getClosedAt()).isNull();
        assertThat(bill.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("Given null table number, When open bill, Then failure")
    void GivenNullTableNumber_WhenOpenBill_ThenFailure() {
        // given
        var waiterInfo = new WaiterInfo("John", "Doe", UUID.randomUUID().toString());
        var lines = List.of(line(UUID.randomUUID().toString(), 1, "10.00", "Item", 1));
        var cmd = new OpenBillCommand(null, waiterInfo, lines);

        // when
        var result = Bill.open(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Table number must be provided");
    }

    @Test
    @DisplayName("Given null waiter info, When open bill, Then failure")
    void GivenNullWaiterInfo_WhenOpenBill_ThenFailure() {
        // given
        var tableNumber = TableNumber.of(5);
        var lines = List.of(line(UUID.randomUUID().toString(), 1, "10.00", "Item", 1));
        var cmd = new OpenBillCommand(tableNumber, null, lines);

        // when
        var result = Bill.open(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Waiter information must be provided");
    }

    @Test
    @DisplayName("Given null initial lines, When open bill, Then failure")
    void GivenNullInitialLines_WhenOpenBill_ThenFailure() {
        // given
        var tableNumber = TableNumber.of(5);
        var waiterInfo = new WaiterInfo("John", "Doe", UUID.randomUUID().toString());
        var cmd = new OpenBillCommand(tableNumber, waiterInfo, null);

        // when
        var result = Bill.open(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Initial bill lines must be provided");
    }

    @Test
    @DisplayName("Given empty initial lines, When open bill, Then failure")
    void GivenEmptyInitialLines_WhenOpenBill_ThenFailure() {
        // given
        var tableNumber = TableNumber.of(5);
        var waiterInfo = new WaiterInfo("John", "Doe", UUID.randomUUID().toString());
        var cmd = new OpenBillCommand(tableNumber, waiterInfo, List.of());

        // when
        var result = Bill.open(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Initial bill lines must be provided");
    }

    @Test
    @DisplayName("Given open bill with items, When close, Then status changed to CLOSED")
    void GivenOpenBillWithItems_WhenClose_ThenStatusClosed() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(3),
                new WaiterInfo("Alice", "Johnson", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "20.00", "Burger", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();

        // when
        var result = bill.close(FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.CLOSED);
        assertThat(bill.getClosedAt()).isNotNull();
        assertThat(bill.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given closed bill, When close again, Then failure")
    void GivenClosedBill_WhenCloseAgain_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(3),
                new WaiterInfo("Bob", "Brown", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "10.00", "Soup", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();

        // when
        var result = bill.close(FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Only open bills can be closed");
    }

    @Test
    @DisplayName("Given paid bill, When close, Then failure")
    void GivenPaidBill_WhenClose_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(3),
                new WaiterInfo("Carol", "White", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "15.00", "Cake", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();
        var payCmd = new PayBillCommand(PaymentMethod.CASH, Money.of("20.00"));
        assertThat(bill.pay(payCmd, FIXED_CLOCK).isSuccess()).isTrue();

        // when
        var result = bill.close(FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Only open bills can be closed");
    }


    @Test
    @DisplayName("Given closed bill with exact amount, When pay with CASH, Then bill paid successfully")
    void GivenClosedBillExactAmount_WhenPayWithCash_ThenBillPaid() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(8),
                new WaiterInfo("Dave", "Green", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "25.50", "Pasta", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();

        var payCmd = new PayBillCommand(PaymentMethod.CASH, Money.of("25.50"));

        // when
        var result = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(bill.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(bill.getPaidAmount().amount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(bill.getPaidAt()).isNotNull();
        assertThat(bill.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given closed bill, When pay with more than total amount, Then bill paid successfully")
    void GivenClosedBill_WhenPayWithMoreThanTotal_ThenBillPaid() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(10),
                new WaiterInfo("Eve", "Black", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 2, "8.00", "Coffee", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();

        var payCmd = new PayBillCommand(PaymentMethod.CARD, Money.of("20.00"));

        // when
        var result = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(bill.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(bill.getPaidAmount().amount()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Given closed bill, When pay with less than total amount, Then failure")
    void GivenClosedBill_WhenPayWithLessThanTotal_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(12),
                new WaiterInfo("Frank", "Gray", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "50.00", "Steak", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();

        var payCmd = new PayBillCommand(PaymentMethod.CASH, Money.of("40.00"));

        // when
        var result = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Payment amount must be equal or greater than total amount");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.CLOSED); // unchanged
    }

    @Test
    @DisplayName("Given open bill, When pay, Then failure")
    void GivenOpenBill_WhenPay_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(15),
                new WaiterInfo("Grace", "Blue", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "5.00", "Tea", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();

        var payCmd = new PayBillCommand(PaymentMethod.CARD, Money.of("10.00"));

        // when
        var result = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Only closed bills can be paid");
    }

    @Test
    @DisplayName("Given paid bill, When pay again, Then failure")
    void GivenPaidBill_WhenPayAgain_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(20),
                new WaiterInfo("Henry", "Red", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "35.00", "Wine", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();
        var firstPayCmd = new PayBillCommand(PaymentMethod.CASH, Money.of("40.00"));
        assertThat(bill.pay(firstPayCmd, FIXED_CLOCK).isSuccess()).isTrue();

        var secondPayCmd = new PayBillCommand(PaymentMethod.CARD, Money.of("50.00"));

        // when
        var result = bill.pay(secondPayCmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Only closed bills can be paid");
    }

    @Test
    @DisplayName("Given closed bill, When pay with null payment method, Then failure")
    void GivenClosedBill_WhenPayWithNullPaymentMethod_ThenFailure() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(25),
                new WaiterInfo("Ivy", "Purple", UUID.randomUUID().toString()),
                List.of(line(UUID.randomUUID().toString(), 1, "12.00", "Dessert", 1))
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();

        var payCmd = new PayBillCommand(null, Money.of("15.00"));

        // when
        var result = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Payment method must be provided");
    }

    @Test
    @DisplayName("Given bill with multiple items, When close and pay, Then full workflow succeeds")
    void GivenBillWithMultipleItems_WhenCloseAndPay_ThenFullWorkflowSucceeds() {
        // given
        var cmd = new OpenBillCommand(
                TableNumber.of(30),
                new WaiterInfo("Jack", "Orange", UUID.randomUUID().toString()),
                List.of(
                        line(UUID.randomUUID().toString(), 2, "10.00", "Item1", 1),
                        line(UUID.randomUUID().toString(), 1, "15.50", "Item2", 1),
                        line(UUID.randomUUID().toString(), 3, "7.00", "Item3", 1)
                )
        );
        var openResult = Bill.open(cmd, FIXED_CLOCK);
        assertThat(openResult.isSuccess()).isTrue();
        var bill = openResult.getValue();

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("56.50"));

        // when
        var closeResult = bill.close(FIXED_CLOCK);
        var payCmd = new PayBillCommand(PaymentMethod.CARD, Money.of("60.00"));
        var payResult = bill.pay(payCmd, FIXED_CLOCK);

        // then
        assertThat(closeResult.isSuccess()).isTrue();
        assertThat(payResult.isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(bill.getPaidAmount().amount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }
}