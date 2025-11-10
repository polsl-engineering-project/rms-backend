package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.bill.cmd.*;
import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.order.vo.PaymentMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class Bill {

    private BillId id;

    private TableNumber tableNumber;

    private BillStatus status;

    private PaymentMethod paymentMethod;

    @Getter(AccessLevel.NONE)
    private final List<BillLine> lines = new ArrayList<>();

    private WaiterInfo waiterInfo;

    private Money totalAmount;

    private Money paidAmount;

    private Instant openedAt;

    private Instant closedAt;

    private Instant paidAt;

    private Instant updatedAt;

    private long version;

    private Bill(
            TableNumber tableNumber,
            List<BillLine> initialLines,
            WaiterInfo waiterInfo,
            Clock clock
    ) {
        this.id = BillId.generate();
        this.tableNumber = tableNumber;
        this.status = BillStatus.OPEN;
        this.lines.addAll(initialLines);
        this.waiterInfo = waiterInfo;
        this.totalAmount = calculateTotal(initialLines);
        this.paidAmount = Money.ZERO;
        this.openedAt = Instant.now(clock);
        this.updatedAt = Instant.now(clock);
        this.version = 0;
    }

    static Bill reconstruct(
            BillId id,
            TableNumber tableNumber,
            BillStatus status,
            PaymentMethod paymentMethod,
            List<BillLine> lines,
            WaiterInfo waiterInfo,
            Money totalAmount,
            Money paidAmount,
            Instant openedAt,
            Instant closedAt,
            Instant paidAt,
            Instant updatedAt,
            long version
    ) {
        var bill = new Bill(
                id,
                tableNumber,
                status,
                paymentMethod,
                waiterInfo,
                totalAmount,
                paidAmount,
                openedAt,
                closedAt,
                paidAt,
                updatedAt,
                version
        );
        bill.lines.addAll(lines);
        return bill;
    }

    static Result<Bill> open(OpenBillCommand cmd, Clock clock) {
        var validationResult = validateBillOpening(cmd);
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.getError());
        }

        var bill = new Bill(
                cmd.tableNumber(),
                cmd.initialLines(),
                cmd.waiterInfo(),
                clock
        );

        return Result.ok(bill);
    }


    private static Result<Void> validateBillOpening(OpenBillCommand cmd) {
        if (cmd.tableNumber() == null) {
            return Result.failure("Table number must be provided");
        }
        if (cmd.waiterInfo() == null) {
            return Result.failure("Waiter information must be provided");
        }
        if (cmd.initialLines() == null || cmd.initialLines().isEmpty()) {
            return Result.failure("Initial bill lines must be provided");
        }

        return Result.ok(null);
    }


    Result<Void> addItems(AddItemsToBillCommand cmd, Clock clock) {
        if (status != BillStatus.OPEN) {
            return Result.failure("Can only add items to an open bill");
        }
        if (cmd.safeGetLines().isEmpty()) {
            return Result.failure("No items to add were provided");
        }

        lines.addAll(cmd.safeGetLines());
        totalAmount = calculateTotal(lines);
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    Result<Void> removeItems(RemoveItemsFromBillCommand cmd, Clock clock) {
        if (status != BillStatus.OPEN) {
            return Result.failure("Can only remove items from an open bill");
        }

        var removeLines = cmd.getRemovedLines();
        if (removeLines.isEmpty()) {
            return Result.failure("No items to remove were provided");
        }

        var removeResult = BillLinesRemover.remove(lines, removeLines);
        if (removeResult.isFailure()) {
            return Result.failure(removeResult.getError());
        }

        lines.clear();
        lines.addAll(removeResult.getValue());

        if (lines.isEmpty()) {
            return Result.failure("Cannot remove all items from bill.");
        }

        totalAmount = calculateTotal(lines);
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    Result<Void> close(Clock clock) {
        if (status != BillStatus.OPEN) {
            return Result.failure("Only open bills can be closed");
        }
        if (lines.isEmpty()) {
            return Result.failure("Cannot close bill with no items");
        }

        status = BillStatus.CLOSED;
        closedAt = Instant.now(clock);
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }

    Result<Void> pay(PayBillCommand cmd, Clock clock) {
        if (status != BillStatus.CLOSED) {
            return Result.failure("Only closed bills can be paid");
        }
        if (lines.isEmpty()) {
            return Result.failure("Cannot pay bill with no items");
        }
        if(cmd.paidAmount().isLessThan(totalAmount)){
            return Result.failure("Payment amount must be equal or greater than total amount");
        }
        if(cmd.paymentMethod() == null) {
            return Result.failure("Payment method must be provided");
        }

        paymentMethod = cmd.paymentMethod();
        paidAmount = cmd.paidAmount();
        status = BillStatus.PAID;
        paidAt = Instant.now(clock);
        updatedAt = Instant.now(clock);

        return Result.ok(null);
    }


    private static Money calculateTotal(List<BillLine> lines) {
        var total = lines.stream()
                .map(line -> line.unitPrice().multiply(line.quantity()))
                .reduce(Money.ZERO, Money::add);
        return total;
    }

    List<BillLine> getLines() {
        return List.copyOf(lines);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bill bill)) return false;
        return Objects.equals(id, bill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}