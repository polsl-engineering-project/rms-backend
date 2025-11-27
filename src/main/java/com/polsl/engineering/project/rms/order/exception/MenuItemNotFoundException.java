package com.polsl.engineering.project.rms.order.exception;

import com.polsl.engineering.project.rms.order.OrderPayloads;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MenuItemNotFoundException extends RuntimeException {
    public static final String MSG_TEMPLATE = "Menu item not found: %s";

    public MenuItemNotFoundException(OrderPayloads.OrderLineRequest orderLine) {
        super(MSG_TEMPLATE.formatted(orderLine.menuItemId()));
    }

    public MenuItemNotFoundException(String menuItemId) {
        super(MSG_TEMPLATE.formatted(menuItemId));
    }
}