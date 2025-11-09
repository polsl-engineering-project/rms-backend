package com.polsl.engineering.project.rms.bill.cmd;

import com.polsl.engineering.project.rms.bill.vo.BillLineRemoval;

import java.util.List;

public record RemoveItemsFromBillCommand(
        List<BillLineRemoval> removedLines
) {
    public List<BillLineRemoval> getRemovedLines() {
        return removedLines != null ? removedLines : List.of();
    }
}
