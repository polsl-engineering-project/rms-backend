package com.polsl.engineering.project.rms.order.cmd;

import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderLineRemoval;

import java.util.List;

public record ChangeOrderLinesCommand(List<OrderLine> newLines, List<OrderLineRemoval> removedLines, int updatedEstimatedPreparationTimeMinutes) {
    public List<OrderLine> safeGetNewLines() {
        return newLines != null ? newLines : List.of();
    }
    public List<OrderLineRemoval> safeGetRemovedLines() {
        return removedLines != null ? removedLines : List.of();
    }
}
