package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.menu.dto.MenuItemSnapshotForOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MenuApi {
    Map<UUID, MenuItemSnapshotForOrder> getSnapshotsForOrderByIds(List<UUID> ids);
}