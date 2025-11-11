package com.polsl.engineering.project.rms.bill.cmd;

import com.polsl.engineering.project.rms.bill.vo.BillLine;

import java.util.List;

public record AddItemsToBillCommand(
        List<BillLine> newLines
) {
    public List<BillLine> safeGetLines() {
        return newLines != null ? newLines : List.of();
    }
}
