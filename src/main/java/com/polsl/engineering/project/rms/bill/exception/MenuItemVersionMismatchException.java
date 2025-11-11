package com.polsl.engineering.project.rms.bill.exception;

import com.polsl.engineering.project.rms.bill.BillPayloads;

public class MenuItemVersionMismatchException extends RuntimeException {
    public MenuItemVersionMismatchException(long actualVersion, BillPayloads.BillLine line) {
        super("Menu item version mismatch for id " + line.menuItemId() +
                ". Expected version: " + line.version() + ", actual version: " + actualVersion);
    }
}
