package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.cmd.*;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import com.polsl.engineering.project.rms.order.vo.OrderCustomerVisibleStatus;

@Mapper(componentModel = "spring")
interface OrderMapper {

    @Mapping(target = "orderLines", source = "orderLines")
    PlaceDeliveryOrderCommand toCommand(OrderPayloads.PlaceDeliveryOrderRequest request, List<OrderLine> orderLines);

    @Mapping(target = "orderLines", source = "orderLines")
    PlacePickUpOrderCommand toCommand(OrderPayloads.PlacePickUpOrderRequest request, List<OrderLine> orderLines);

    ApproveOrderByKitchenCommand toCommand(OrderPayloads.ApproveOrderByKitchenRequest request);

    CancelOrderCommand toCommand(OrderPayloads.CancelOrderRequest request);

    @Mapping(target = "id", source = "id")
    OrderPayloads.OrderPlacedResponse toResponse(Order order);

    default UUID map(OrderId value) {
        return value == null ? null : value.value();
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", expression = "java(order.getStatus() == null ? null : order.getStatus().name())")
    @Mapping(target = "customerInfo", source = "customerInfo")
    @Mapping(target = "address", source = "deliveryAddress")
    @Mapping(target = "deliveryMode", source = "deliveryMode")
    @Mapping(target = "scheduledFor", source = "scheduledFor")
    @Mapping(target = "orderLines", expression = "java(mapLines(order.getLines()))")
    @Mapping(target = "estimatedPreparationTimeMinutes", source = "estimatedPreparationMinutes")
    OrderPayloads.OrderDetailsResponse toDetailsResponse(Order order);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", expression = "java(mapStatusToCustomerVisible(order.getStatus()))")
    @Mapping(target = "estimatedPreparationMinutes", source = "estimatedPreparationMinutes")
    @Mapping(target = "cancellationReason", source = "cancellationReason")
    OrderPayloads.OrderCustomerViewResponse toCustomerViewResponse(Order order);

    @SuppressWarnings("unused")
    default String mapStatusToCustomerVisible(OrderStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PENDING_APPROVAL, APPROVED_BY_FRONT_DESK -> OrderCustomerVisibleStatus.PENDING_APPROVAL.name();
            case CONFIRMED -> OrderCustomerVisibleStatus.CONFIRMED.name();
            case READY_FOR_PICKUP, READY_FOR_DRIVER -> OrderCustomerVisibleStatus.READY_FOR_PICKUP.name();
            case IN_DELIVERY -> OrderCustomerVisibleStatus.IN_DELIVERY.name();
            case COMPLETED -> OrderCustomerVisibleStatus.COMPLETED.name();
            case CANCELLED -> OrderCustomerVisibleStatus.CANCELLED.name();
        };
    }

    @Mapping(target = "menuItemId", expression = "java(java.util.UUID.fromString(line.menuItemId()))")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "version", source = "menuItemVersion")
    OrderPayloads.OrderLine toPayloadOrderLine(OrderLine line);

    @SuppressWarnings("unused")
    default List<OrderPayloads.OrderLine> mapLines(List<OrderLine> lines) {
        if (lines == null) return java.util.List.of();
        return lines.stream()
                .map(this::toPayloadOrderLine)
                .toList();
    }

}
