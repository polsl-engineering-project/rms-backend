package com.polsl.engineering.project.rms.bill.cmd;

import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.TableNumber;
import com.polsl.engineering.project.rms.bill.vo.WaiterInfo;

import java.util.List;

public record OpenBillCommand(
        TableNumber tableNumber,
        WaiterInfo waiterInfo,
        List<BillLine> initialLines
) {
}
