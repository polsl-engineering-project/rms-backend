package com.polsl.engineering.project.rms.bill.exception;

import com.polsl.engineering.project.rms.bill.BillPayloads;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException(BillPayloads.BillLine line) {
        super("Menu item with id " + line.menuItemId() + " not found");
    }
}
