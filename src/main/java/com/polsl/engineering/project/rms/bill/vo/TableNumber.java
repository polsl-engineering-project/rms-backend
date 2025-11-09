package com.polsl.engineering.project.rms.bill.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TableNumber(
        @NotNull @Min(1) Integer value
) {
    public static TableNumber of(Integer value) {
        return new TableNumber(value);
    }
}
