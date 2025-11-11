package com.polsl.engineering.project.rms.bill.cmd;

import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.TableNumber;

import java.util.List;

public record OpenBillCommand(
        TableNumber tableNumber,
        String userId,
        List<BillLine> initialLines
) {
}
