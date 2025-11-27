package com.polsl.engineering.project.rms.bill.vo;

public record BillLine(
        String menuItemId,
        int quantity,
        Money unitPrice,
        String menuItemName
) {
    public BillLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
    }
}
