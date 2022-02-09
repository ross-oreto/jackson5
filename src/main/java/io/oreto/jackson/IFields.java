package io.oreto.jackson;


public interface IFields {
    /**
     * @return The field to set as the tree root
     */
    default String root() { return null; }

    /**
     * @return The fields to include in the serialization
     */
    default String include() { return null; }

    /**
     * @return The fields to exclude in the serialization
     */
    default String exclude() { return null; }
}
