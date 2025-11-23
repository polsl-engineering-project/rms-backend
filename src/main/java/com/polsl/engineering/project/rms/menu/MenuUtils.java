package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.general.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.menu.exception.UuidCorruptionException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.NONE)
class MenuUtils {
    public static void validateIdOrThrow(UUID id, UUID requestId) {
        if (!id.equals(requestId)) {
            throw new UuidCorruptionException();
        }
    }

    public static UUID toUUIDOrThrow(String strId) {
        try {
            return UUID.fromString(strId);
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDFormatException(strId);
        }
    }
}
