package io.oreto.jackson;


public interface Structurable {
    default String view() { return null; }
    default String select() { return null; }
    default String drop() { return null; }
}
