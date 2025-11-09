package com.polsl.engineering.project.rms.bill.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiterInfo(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 50) String employeeId
) {
}
