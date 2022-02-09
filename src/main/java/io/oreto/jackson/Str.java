package io.oreto.jackson;

import java.util.Optional;

/**
 * String utils class
 */
class Str {
    /**
     * Is String s null or empty
     * @param s The string to evaluate
     * @return boolean true if null or empty, false otherwise
     */
    static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * Is String s blank
     * @param s The string to evaluate
     * @return boolean true if string is blank, meaning only whitespace. False otherwise
     */
    static boolean isBlank(CharSequence s) {
        return isEmpty(s) || s.chars().allMatch(Character::isWhitespace);
    }

    /**
     * Is String s not blank
     * @param s The string to evaluate
     * @return boolean true if string is not blank, meaning something other than whitespace. False otherwise
     */
    static boolean isNotBlank(CharSequence s) {
        return !isBlank(s);
    }

    /**
     * Convert String s to an Integer if possible
     * @param s The String representation of a number
     * @return Optional Integer if s in a number, Optional.empty otherwise
     */
    static Optional<Integer> toInteger(CharSequence s) {
        try {
            return Optional.of(Integer.parseInt(s.toString()));
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }
}
