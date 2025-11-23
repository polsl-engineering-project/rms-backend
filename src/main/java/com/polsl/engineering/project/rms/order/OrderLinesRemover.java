package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.result.Result;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderLineRemoval;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.NONE)
class OrderLinesRemover {

    public static Result<List<OrderLine>> remove(List<OrderLine> currentLines, List<OrderLineRemoval> removeLines) {
        var sortedCurrentLines = sortByVersionDesc(currentLines);
        var snapshot = prepareSortedSnapshot(sortedCurrentLines);

        return applyRemovals(snapshot, sortedCurrentLines, removeLines);
    }

    private static ArrayList<OrderLine> prepareSortedSnapshot(List<OrderLine> sortedCurrentLines) {
        return new ArrayList<>(sortedCurrentLines);
    }

    private static Result<List<OrderLine>> applyRemovals(
            List<OrderLine> snapshot,
            List<OrderLine> sortedCurrentLines,
            List<OrderLineRemoval> removeLines
    ) {
        for (var removeLine : removeLines) {
            var res = applyRemovalForMenuItem(snapshot, sortedCurrentLines, removeLine);
            if (res.isFailure()) {
                return Result.failure(res.getError());
            }
        }
        return Result.ok(snapshot);
    }

    private static Result<Void> applyRemovalForMenuItem(
            List<OrderLine> snapshot,
            List<OrderLine> sortedCurrentLines,
            OrderLineRemoval removeLine
    ) {
        var toBeRemovedLeft = removeLine.quantity();
        var lineFound = false;

        for (var currentLine : sortedCurrentLines) {
            if (currentLine.menuItemId().equals(removeLine.menuItemId())) {
                lineFound = true;

                var quantityToRemove = Math.min(currentLine.quantity(), toBeRemovedLeft);
                removeOrReplaceLine(snapshot, currentLine, quantityToRemove);

                toBeRemovedLeft -= quantityToRemove;
                if (toBeRemovedLeft <= 0) {
                    break;
                }
            }
        }

        return validateRemovalCompleted(lineFound, toBeRemovedLeft, removeLine.menuItemId());
    }

    private static void removeOrReplaceLine(List<OrderLine> snapshot, OrderLine currentLine, int quantityToRemove) {
        if (quantityToRemove == currentLine.quantity()) {
            snapshot.remove(currentLine);
        } else {
            var left = currentLine.quantity() - quantityToRemove;
            var newLine = createUpdatedLine(currentLine, left);
            snapshot.remove(currentLine);
            snapshot.add(newLine);
        }
    }

    private static Result<Void> validateRemovalCompleted(boolean lineFound, int toBeRemovedLeft, String menuItemId) {
        if (!lineFound) {
            return Result.failure("Menu item id: " + menuItemId + " not found in order.");
        }
        if (toBeRemovedLeft > 0) {
            return Result.failure("Cannot remove more quantity than exists for menu item id: " + menuItemId);
        }
        return Result.ok(null);
    }

    private static List<OrderLine> sortByVersionDesc(List<OrderLine> currentLines) {
        return currentLines.stream()
                .sorted((line1, line2) -> Long.compare(line2.menuItemVersion(), line1.menuItemVersion()))
                .toList();
    }

    private static OrderLine createUpdatedLine(OrderLine currentLine, int leftQuantity) {
        return new OrderLine(
                currentLine.menuItemId(),
                leftQuantity,
                currentLine.unitPrice(),
                currentLine.menuItemVersion()
        );
    }
}
