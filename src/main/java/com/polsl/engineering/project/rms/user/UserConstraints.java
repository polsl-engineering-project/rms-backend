package com.polsl.engineering.project.rms.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class UserConstraints {

    public static final int USERNAME_MAX_LENGTH = 100;
    public static final int USERNAME_MIN_LENGTH = 3;

    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int FIRST_NAME_MAX_LENGTH = 100;
    public static final int FIRST_NAME_MIN_LENGTH = 3;

    public static final int LAST_NAME_MAX_LENGTH = 100;
    public static final int LAST_NAME_MIN_LENGTH = 2;

    public static final int PHONE_NUMBER_MAX_LENGTH = 32;
    public static final int PHONE_NUMBER_MIN_LENGTH = 9;

}
