package com.polsl.engineering.project.rms.bill.cmd;

import com.polsl.engineering.project.rms.bill.vo.Money;
import com.polsl.engineering.project.rms.order.vo.PaymentMethod;


public record PayBillCommand(
        PaymentMethod paymentMethod,
        Money paidAmount
) {
}
