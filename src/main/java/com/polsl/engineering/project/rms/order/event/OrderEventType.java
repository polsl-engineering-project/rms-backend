package com.polsl.engineering.project.rms.order.event;

public enum OrderEventType {
    INITIAL_DATA,
    DELIVERY_ORDER_PLACED,
    APPROVED,
    CANCELLED,
    COMPLETED,
    DELIVERY_STARTED,
    LINES_CHANGED,
    MARKED_AS_READY,
    PICK_UP_ORDER_PLACED
}
