package com.polsl.engineering.project.rms.order.cmd;

import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderLine;

import java.time.LocalTime;
import java.util.List;

public record PlacePickUpOrderCommand(
        CustomerInfo customerInfo,
        DeliveryMode deliveryMode,
        LocalTime scheduledFor,
        List<OrderLine> orderLines
) {
}
