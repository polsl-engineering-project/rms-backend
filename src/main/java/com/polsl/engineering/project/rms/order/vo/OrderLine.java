package com.polsl.engineering.project.rms.order.vo;

public record OrderLine(
        String menuItemId,
        int quantity,
        Money unitPrice,
        long menuItemVersion
) {
}