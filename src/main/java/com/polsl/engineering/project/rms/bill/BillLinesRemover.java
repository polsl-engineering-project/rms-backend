package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.common.result.Result;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.BillLineRemoval;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.NONE)
class BillLinesRemover {

    public static Result<List<BillLine>> remove(List<BillLine> currentLines, List<BillLineRemoval> removeLines) {
        var sortedCurrentLines = sortByVersionDesc(currentLines);
        var snapshot = prepareSortedSnapshot(sortedCurrentLines);

        return applyRemovals(snapshot, sortedCurrentLines, removeLines);
    }

    private static ArrayList<BillLine> prepareSortedSnapshot(List<BillLine> sortedCurrentLines) {
        return new ArrayList<>(sortedCurrentLines);
    }

    private static Result<List<BillLine>> applyRemovals(
            List<BillLine> snapshot,
            List<BillLine> sortedCurrentLines,
            List<BillLineRemoval> removeLines
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
            List<BillLine> snapshot,
            List<BillLine> sortedCurrentLines,
            BillLineRemoval removeLine
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

    private static void removeOrReplaceLine(List<BillLine> snapshot, BillLine currentLine, int quantityToRemove) {
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
            return Result.failure("Menu item id: " + menuItemId + " not found in bill");
        }
        if (toBeRemovedLeft > 0) {
            return Result.failure("Cannot remove more quantity than exists for menu item id: " + menuItemId);
        }
        return Result.ok(null);
    }

    private static List<BillLine> sortByVersionDesc(List<BillLine> currentLines) {
        return currentLines.stream()
                .sorted((line1, line2) -> Long.compare(line2.menuItemVersion(), line1.menuItemVersion()))
                .toList();
    }

    private static BillLine createUpdatedLine(BillLine currentLine, int leftQuantity) {
        return new BillLine(
                currentLine.menuItemId(),
                leftQuantity,
                currentLine.unitPrice(),
                currentLine.menuItemVersion()
        );
    }
}
