package com.polsl.engineering.project.rms.order.vo;

public enum OrderStatus {
    PENDING_APPROVAL,
    APPROVED_BY_FRONT_DESK,
    CONFIRMED,
    READY_FOR_PICKUP,
    READY_FOR_DRIVER,
    IN_DELIVERY,
    COMPLETED,
    CANCELLED
}