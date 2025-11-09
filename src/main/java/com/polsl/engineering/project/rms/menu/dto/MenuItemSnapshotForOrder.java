package com.polsl.engineering.project.rms.menu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemSnapshotForOrder(UUID id, BigDecimal price, long version) {
}
