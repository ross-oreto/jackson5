package io.oreto.jackson;


public interface Structurable {
    /**
     * @return The field to set as the tree root
     */
    default String view() { return null; }

    /**
     * @return The fields to include in the serialization
     */
    default String select() { return null; }

    /**
     * @return The fields to exclude in the serialization
     */
    default String drop() { return null; }
}
