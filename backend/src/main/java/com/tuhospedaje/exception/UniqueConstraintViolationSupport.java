package com.tuhospedaje.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/**
 * Inspects exception cause chains to distinguish database unique-constraint violations
 * (concurrent duplicate races) from non-duplicate integrity errors such as foreign-key,
 * NOT NULL, or CHECK constraints.
 */
public final class UniqueConstraintViolationSupport {

    private UniqueConstraintViolationSupport() {
    }

    /**
     * Returns {@code true} if {@code throwable} or any cause in its chain corresponds to
     * {@code constraintName} (case-insensitive, allowing schema/table prefixes or quotes).
     */
    public static boolean matchesConstraint(@Nullable Throwable throwable, String constraintName) {
        if (throwable == null || constraintName.isBlank()) {
            return false;
        }

        boolean sawNamedConstraint = false;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current instanceof ConstraintViolationException cve) {
                String actual = cve.getConstraintName();
                if (actual != null && !actual.isBlank()) {
                    sawNamedConstraint = true;
                    if (matchesConstraintName(actual, constraintName)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }

        if (sawNamedConstraint) {
            return false;
        }

        // Fallback for non-Hibernate integrity exceptions or dialects that omit constraintName.
        String needle = stripDelimiters(constraintName).toLowerCase(Locale.ROOT);
        visited.clear();
        current = throwable;
        while (current != null && visited.add(current)) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean matchesConstraintName(String actual, String expected) {
        String normalizedActual = stripDelimiters(actual).toLowerCase(Locale.ROOT);
        String normalizedExpected = stripDelimiters(expected).toLowerCase(Locale.ROOT);
        return normalizedActual.equals(normalizedExpected)
                || normalizedActual.endsWith("." + normalizedExpected);
    }

    private static String stripDelimiters(String value) {
        return value
                .replace("`", "")
                .replace("\"", "")
                .replace("'", "")
                .trim();
    }
}
