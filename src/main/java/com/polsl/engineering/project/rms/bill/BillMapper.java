package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand;
import com.polsl.engineering.project.rms.bill.vo.BillId;
import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.TableNumber;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
interface BillMapper {

    @Mapping(target = "tableNumber", source = "request.tableNumber")
    @Mapping(target = "userId", source = "request.userId")
    @Mapping(target = "initialLines", source = "billLines")
    OpenBillCommand toCommand(BillPayloads.OpenBillRequest request, List<BillLine> billLines);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tableNumber", source = "tableNumber.value")
    BillPayloads.BillOpenedResponse toResponse(Bill bill);

    @Mapping(target = "id", source = "bill.id.value")
    @Mapping(target = "tableNumber", source = "bill.tableNumber.value")
    @Mapping(target = "userId", expression = "java(bill.getUserId())")
    @Mapping(target = "totalAmount", expression = "java(bill.getTotalAmount() != null ? bill.getTotalAmount().amount() : null)")
    @Mapping(target = "billLines", source = "lines")
    BillPayloads.BillSummaryWithLinesResponse toSummaryWithLinesResponse(Bill bill, List<BillPayloads.BillLineResponse> lines);

    default UUID map(BillId value) {
        return value == null ? null : value.value();
    }

    default TableNumber map(Integer value) {
        return value == null ? null : TableNumber.of(value);
    }
}
