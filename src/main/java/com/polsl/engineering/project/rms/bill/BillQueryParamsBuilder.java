package com.polsl.engineering.project.rms.bill;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Objects;

@Component
class BillQueryParamsBuilder {

    Object[] buildInsertParams(Bill bill) {
        Objects.requireNonNull(bill, "bill cannot be null");
        var id = bill.getId().value();
        var common = buildCommonParams(bill);
        var params = new Object[common.length + 2];
        params[0] = id;
        System.arraycopy(common, 0, params, 1, common.length);
        params[params.length - 1] = bill.getVersion();
        return params;
    }

    Object[] buildUpdateParams(Bill bill) {
        Objects.requireNonNull(bill, "bill cannot be null");
        var id = bill.getId().value();
        var common = buildCommonParams(bill);
        var params = new Object[common.length + 2];
        System.arraycopy(common, 0, params, 0, common.length);
        params[common.length] = id;
        params[common.length + 1] = bill.getVersion();
        return params;
    }

    private Object[] buildCommonParams(Bill bill) {
        var openedAt = bill.getOpenedAt() == null ? null : Timestamp.from(bill.getOpenedAt());
        var closedAt = bill.getClosedAt() == null ? null : Timestamp.from(bill.getClosedAt());
        var paidAt = bill.getPaidAt() == null ? null : Timestamp.from(bill.getPaidAt());
        var updatedAt = bill.getUpdatedAt() == null ? null : Timestamp.from(bill.getUpdatedAt());
        var paymentMethod = bill.getPaymentMethod() == null ? null : bill.getPaymentMethod().name();
        var paidAmount = bill.getPaidAmount() == null ? null : bill.getPaidAmount().amount();

        return new Object[]{
                bill.getTableNumber().value(),
                bill.getStatus().name(),
                paymentMethod,
                bill.getWaiterInfo().firstName(),
                bill.getWaiterInfo().lastName(),
                bill.getWaiterInfo().employeeId(),
                bill.getTotalAmount().amount(),
                paidAmount,
                openedAt,
                closedAt,
                paidAt,
                updatedAt
        };
    }
}