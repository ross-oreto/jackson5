package io.oreto.jackson.latte;

import java.util.Optional;

public class Str {
    public static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }
    public static boolean isBlank(CharSequence s) {
        return isEmpty(s) || s.chars().allMatch(Character::isWhitespace);
    }
    public static boolean isNotBlank(CharSequence s) {
        return !isBlank(s);
    }

    public static Optional<Integer> toInteger(CharSequence s) {
        try {
            return Optional.of(Integer.parseInt(s.toString()));
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }
}
