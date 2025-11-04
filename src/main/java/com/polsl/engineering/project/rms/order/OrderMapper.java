package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.*;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;
import com.polsl.engineering.project.rms.order.vo.OrderId;

@Mapper(componentModel = "spring")
interface OrderMapper {

    @Mapping(target = "orderLines", source = "orderLines")
    PlaceDeliveryOrderCommand toCommand(OrderPayloads.PlaceDeliveryOrderRequest request, List<OrderLine> orderLines);

    @Mapping(target = "orderLines", source = "orderLines")
    PlacePickUpOrderCommand toCommand(OrderPayloads.PlacePickUpOrderRequest request, List<OrderLine> orderLines);

    @Mapping(target = "newLines", source = "orderLines")
    AddItemsByStaffCommand toCommand(OrderPayloads.AddItemsByStaffRequest request, List<OrderLine> orderLines);

    ApproveOrderByKitchenCommand toCommand(OrderPayloads.ApproveOrderByKitchenRequest request);

    CancelOrderCommand toCommand(OrderPayloads.CancelOrderRequest request);

    @Mapping(target = "id", source = "id")
    OrderPayloads.OrderPlacedResponse toResponse(Order order);

    default UUID map(OrderId value) {
        return value == null ? null : value.value();
    }

}
