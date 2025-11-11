package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand;
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

@DisplayName("Bill.addItems")
class BillAddItemsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);

    private static Bill createOpenBill(List<BillLine> initialLines) {
        var cmd = new OpenBillCommand(
                TableNumber.of(5),
                UUID.randomUUID().toString(),
                initialLines
        );
        var result = Bill.open(cmd, FIXED_CLOCK);
        assertThat(result.isSuccess()).isTrue();
        return result.getValue();
    }

    private static BillLine line(String id, int qty, String price, String name, long version) {
        return new BillLine(id, qty, new Money(new BigDecimal(price)), name, version);
    }

    @Test
    @DisplayName("Given open bill, When adding new items, Then items added and total updated")
    void GivenOpenBill_WhenAddNewItems_ThenItemsAddedAndTotalUpdated() {
        // given
        var pizzaId = UUID.randomUUID().toString();
        var saladId = UUID.randomUUID().toString();
        var pastaId = UUID.randomUUID().toString();

        var bill = createOpenBill(List.of(
                line(pizzaId, 1, "30.00", "Pizza", 1)
        ));
        var initialTotal = bill.getTotalAmount();
        assertThat(initialTotal.amount()).isEqualByComparingTo(new BigDecimal("30.00"));

        var newLines = List.of(
                line(pastaId, 2, "25.50", "Pasta", 1),
                line(saladId, 1, "12.00", "Salad", 1)
        );
        var cmd = new AddItemsToBillCommand(newLines);

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getLines())
                .hasSize(3)
                .extracting(BillLine::menuItemId, BillLine::quantity)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(pizzaId, 1),
                        org.assertj.core.groups.Tuple.tuple(pastaId, 2),
                        org.assertj.core.groups.Tuple.tuple(saladId, 1)
                );

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("93.00"));
        assertThat(bill.getStatus()).isEqualTo(BillStatus.OPEN);
    }

    @Test
    @DisplayName("Given open bill, When adding multiple same items, Then all items added correctly")
    void GivenOpenBill_WhenAddMultipleSameItems_ThenAllItemsAdded() {
        // given
        var burgerId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(burgerId, 1, "20.00", "Burger", 1)
        ));

        var newLines = List.of(
                line(burgerId, 2, "20.00", "Burger", 1),
                line(burgerId, 1, "22.00", "Burger", 2) // newer version, different price
        );
        var cmd = new AddItemsToBillCommand(newLines);

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getLines()).hasSize(3);

        var burgerLines = bill.getLines().stream()
                .filter(l -> l.menuItemId().equals(burgerId))
                .toList();
        assertThat(burgerLines).hasSize(3);

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("82.00"));
    }

    @Test
    @DisplayName("Given closed bill, When adding items, Then failure")
    void GivenClosedBill_WhenAddItems_ThenFailure() {
        // given
        var soupId = UUID.randomUUID().toString();
        var teaId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(soupId, 1, "10.00", "Soup", 1)
        ));
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.CLOSED);

        var cmd = new AddItemsToBillCommand(List.of(
                line(teaId, 1, "5.00", "Tea", 1)
        ));

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Can only add items to an open bill");
        assertThat(bill.getLines()).hasSize(1); // unchanged
    }

    @Test
    @DisplayName("Given empty list of items, When adding items, Then failure")
    void GivenEmptyList_WhenAddItems_ThenFailure() {
        // given
        var wrapId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(wrapId, 1, "18.00", "Wrap", 1)
        ));

        var cmd = new AddItemsToBillCommand(List.of());

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("No items to add were provided");
        assertThat(bill.getLines()).hasSize(1); // unchanged
    }

    @Test
    @DisplayName("Given null command lines, When adding items, Then failure")
    void GivenNullCommandLines_WhenAddItems_ThenFailure() {
        // given
        var sodaId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(sodaId, 1, "5.00", "Soda", 1)
        ));

        var cmd = new AddItemsToBillCommand(null);

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("No items to add were provided");
    }

    @Test
    @DisplayName("Given open bill with zero total, When adding items, Then total calculated correctly")
    void GivenBillWithZeroTotal_WhenAddItems_ThenTotalCalculated() {
        // given
        var freeItemId = UUID.randomUUID().toString();
        var coffeeId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(freeItemId, 1, "0.00", "Free Item", 1)
        ));
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(BigDecimal.ZERO);

        var newLines = List.of(
                line(coffeeId, 3, "8.50", "Coffee", 1)
        );
        var cmd = new AddItemsToBillCommand(newLines);

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    @DisplayName("Given open bill, When adding large quantity, Then total calculated correctly")
    void GivenOpenBill_WhenAddLargeQuantity_ThenTotalCorrect() {
        // given
        var drinkId = UUID.randomUUID().toString();
        var cookieId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(drinkId, 1, "6.00", "Drink", 1)
        ));

        var newLines = List.of(
                line(cookieId, 50, "2.50", "Cookie", 1)
        );
        var cmd = new AddItemsToBillCommand(newLines);

        // when
        var result = bill.addItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("131.00"));
    }
}