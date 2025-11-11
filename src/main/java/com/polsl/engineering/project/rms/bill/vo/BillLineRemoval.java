package com.polsl.engineering.project.rms.bill.vo;

public record BillLineRemoval(
        String menuItemId,
        int quantity
) {
    public BillLineRemoval {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }
    }
}
