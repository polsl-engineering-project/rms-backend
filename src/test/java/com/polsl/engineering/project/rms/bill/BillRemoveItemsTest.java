package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.cmd.RemoveItemsFromBillCommand;
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

@DisplayName("Bill.removeItems")
class BillRemoveItemsTest {

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

    private static BillLine line(String id, int qty, String price, String name) {
        return new BillLine(id, qty, new Money(new BigDecimal(price)), name);
    }

    private static BillLineRemoval lineRemoval(String id, int qty) {
        return new BillLineRemoval(id, qty);
    }

    @Test
    @DisplayName("Given open bill with items, When removing exact quantity, Then item removed and total updated")
    void GivenOpenBill_WhenRemoveExactQuantity_ThenItemRemovedAndTotalUpdated() {
        // given
        var pizzaId = UUID.randomUUID().toString();
        var saladId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(pizzaId, 2, "30.00", "Pizza"),
                line(saladId, 1, "12.00", "Salad")
        ));
        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("72.00"));

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(saladId, 1)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getLines())
                .hasSize(1)
                .extracting(BillLine::menuItemId)
                .containsExactly(pizzaId);

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("Given open bill, When remove more quantity than exists, Then failure and no changes")
    void GivenOpenBill_WhenRemoveMoreThanExists_ThenFailureAndNoChanges() {
        // given
        var pastaId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(pastaId, 2, "25.00", "Pasta")
        ));
        var initialTotal = bill.getTotalAmount();

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(pastaId, 5)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Cannot remove more quantity than exists for menu item id: " + pastaId);
        assertThat(bill.getLines()).hasSize(1);
        assertThat(bill.getTotalAmount()).isEqualTo(initialTotal);
    }

    @Test
    @DisplayName("Given open bill, When remove non-existent item, Then failure")
    void GivenOpenBill_WhenRemoveNonExistentItem_ThenFailure() {
        // given
        var soupId =  UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(soupId, 1, "10.00", "Soup")
        ));

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval("non-existent-id", 1)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Menu item id: non-existent-id not found in bill");
        assertThat(bill.getLines()).hasSize(1);
    }

    @Test
    @DisplayName("Given open bill, When attempting to remove all items, Then failure")
    void GivenOpenBill_WhenRemoveAllItems_ThenFailure() {
        // given
        var drinkId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(drinkId, 3, "6.00", "Drink")
        ));

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(drinkId, 3)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cannot remove all items from bill.");
        assertThat(bill.getLines()).hasSize(0); // removed lines but later won't be saved in BillService
    }

    @Test
    @DisplayName("Given closed bill, When removing items, Then failure")
    void GivenClosedBill_WhenRemoveItems_ThenFailure() {
        // given
        var cakeId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(cakeId, 2, "15.00", "Cake")
        ));
        assertThat(bill.close(FIXED_CLOCK).isSuccess()).isTrue();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.CLOSED);

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(cakeId, 1)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Can only remove items from an open bill");
    }

    @Test
    @DisplayName("Given empty removal list, When removing items, Then failure")
    void GivenEmptyRemovalList_WhenRemoveItems_ThenFailure() {
        // given
        var wrapId =  UUID.randomUUID().toString();
        var sodaId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(wrapId, 1, "18.00", "Wrap"),
                line(sodaId, 1, "5.00", "Soda")
        ));

        var cmd = new RemoveItemsFromBillCommand(List.of());

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("No items to remove were provided");
        assertThat(bill.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("Given open bill with multiple items, When removing partial quantity, Then correct item partially removed")
    void GivenMultipleItems_WhenRemovePartialQuantity_ThenPartiallyRemoved() {
        // given
        var coffeeId = UUID.randomUUID().toString();
        var cookieId = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(coffeeId, 5, "8.50", "Coffee"),
                line(cookieId, 3, "2.50", "Cookie")
        ));

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(coffeeId, 3)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getLines())
                .hasSize(2)
                .extracting(BillLine::menuItemId, BillLine::quantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(coffeeId, 2),
                        org.assertj.core.groups.Tuple.tuple(cookieId, 3)
                );

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("24.50"));
    }

    @Test
    @DisplayName("Given bill with multiple different items, When removing multiple items, Then all removed correctly")
    void GivenMultipleDifferentItems_WhenRemoveMultiple_ThenAllRemovedCorrectly() {
        // given
        var item1Id = UUID.randomUUID().toString();
        var item2Id = UUID.randomUUID().toString();
        var item3Id = UUID.randomUUID().toString();
        var bill = createOpenBill(List.of(
                line(item1Id, 2, "10.00", "Item1"),
                line(item2Id, 1, "15.00", "Item2"),
                line(item3Id, 3, "5.00", "Item3")
        ));

        var cmd = new RemoveItemsFromBillCommand(List.of(
                lineRemoval(item2Id, 1),
                lineRemoval(item3Id, 2)
        ));

        // when
        var result = bill.removeItems(cmd, FIXED_CLOCK);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(bill.getLines())
                .hasSize(2)
                .extracting(BillLine::menuItemId, BillLine::quantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(item1Id, 2),
                        org.assertj.core.groups.Tuple.tuple(item3Id, 1)
                );

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }
}