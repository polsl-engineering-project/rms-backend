package com.polsl.engineering.project.rms.order.cmd;

import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderLine;

import java.time.LocalTime;
import java.util.List;

public record PlaceDeliveryOrderCommand(
        CustomerInfo customerInfo,
        Address address,
        DeliveryMode deliveryMode,
        LocalTime scheduledFor,
        List<OrderLine> orderLines
) {
}
