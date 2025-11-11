package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.vo.*;
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
        var waiterInfo = UUID.randomUUID().toString();
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
        assertThat(bill.getUserId()).isEqualTo(waiterInfo);
        assertThat(bill.getLines()).hasSize(2);
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("72.00"));
        assertThat(bill.getOpenedAt()).isNotNull();
        assertThat(bill.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("Given null table number, When open bill, Then failure")
    void GivenNullTableNumber_WhenOpenBill_ThenFailure() {
        // given
        var waiterInfo = UUID.randomUUID().toString();
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
        assertThat(result.getError()).isEqualTo("UserId information must be provided");
    }

    @Test
    @DisplayName("Given null initial lines, When open bill, Then failure")
    void GivenNullInitialLines_WhenOpenBill_ThenFailure() {
        // given
        var tableNumber = TableNumber.of(5);
        var waiterInfo = UUID.randomUUID().toString();
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
        var waiterInfo = UUID.randomUUID().toString();
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
                UUID.randomUUID().toString(),
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
                UUID.randomUUID().toString(),
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
}