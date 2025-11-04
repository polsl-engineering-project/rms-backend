package com.polsl.engineering.project.rms.menu;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class MenuConstraints {
    public static final int CATEGORY_NAME_MAX_LENGTH = 200;
    public static final int CATEGORY_NAME_MIN_LENGTH = 3;

    public static final int CATEGORY_DESCRIPTION_MAX_LENGTH = 500;
    public static final int CATEGORY_DESCRIPTION_MIN_LENGTH = 0;

    public static final int ITEM_NAME_MAX_LENGTH = 200;
    public static final int ITEM_NAME_MIN_LENGTH = 3;

    public static final int ITEM_DESCRIPTION_MAX_LENGTH = 500;
    public static final int ITEM_DESCRIPTION_MIN_LENGTH = 0;

    public static final int ITEM_ALLERGENS_MAX_LENGTH = 500;
    public static final int ITEM_ALLERGENS_MIN_LENGTH = 0;
}
