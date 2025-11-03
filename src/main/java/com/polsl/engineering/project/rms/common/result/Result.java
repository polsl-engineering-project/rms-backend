package com.polsl.engineering.project.rms.common.result;

import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T> {

    record Success<T>(T value) implements Result<T> {
    }

    record Failure<T>(String error) implements Result<T> {
        public Failure {
            if (error == null || error.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be null or blank");
            }
        }
    }

    static <T> Result<T> ok(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    default Optional<T> getValue() {
        return this instanceof Success<T>(T value) ? Optional.of(value) : Optional.empty();
    }

    default Optional<String> getError() {
        return this instanceof Failure<T>(String error) ? Optional.of(error) : Optional.empty();
    }

    default <U> Result<U> map(Function<T, U> mapper) {
        return this instanceof Success<T>(T value)
                ? Result.ok(mapper.apply(value))
                : Result.failure(((Failure<T>) this).error());
    }

    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return this instanceof Success<T>(T value)
                ? mapper.apply(value)
                : Result.failure(((Failure<T>) this).error());
    }
}
