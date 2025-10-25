package com.polsl.engineering.project.rms;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class MockitoAssertJMatchers {

    public static <T> T recursiveEq(T expected, String... ignoreFields) {
        return argThat(new RecursiveEqualsMatcher<>(expected, Arrays.asList(ignoreFields)));
    }

    private record RecursiveEqualsMatcher<T>(T expected, List<String> ignoreFields) implements ArgumentMatcher<T> {
        @Override
        public boolean matches(T actual) {
            try {
                Assertions.assertThat(actual)
                        .usingRecursiveComparison()
                        .ignoringFields(ignoreFields.toArray(String[]::new))
                        .isEqualTo(expected);
                return true;
            } catch (AssertionError _) {
                return false;
            }
        }

        @Override
        public @NotNull String toString() {
            if (ignoreFields.isEmpty()) {
                return "recursively equal to " + expected;
            }
            return "recursively equal to " + expected + " ignoring fields " + ignoreFields;
        }
    }

}
