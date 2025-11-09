package com.polsl.engineering.project.rms.order.exception;

import com.polsl.engineering.project.rms.order.OrderPayloads;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MenuItemVersionMismatchException extends RuntimeException {
    private static final String MSG_TEMPLATE = "Menu item version mismatch for ID: %s. Expected version: %d, actual version: %d";

    public MenuItemVersionMismatchException(long actualVersion, OrderPayloads.OrderLine orderLine) {
        super(MSG_TEMPLATE.formatted(orderLine.menuItemId(), orderLine.version(), actualVersion));
    }
}