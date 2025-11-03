package com.polsl.engineering.project.rms.order.cmd;

import com.polsl.engineering.project.rms.order.vo.OrderLine;

import java.util.List;

public record AddItemsByStaffCommand(
        List<OrderLine> newLines,
        Integer updatedEstimatedMinutes
) {
}
