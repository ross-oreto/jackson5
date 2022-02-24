package io.oreto.jackson;

public class NoSuchJackson5 extends Exception {
    public NoSuchJackson5(String message) {
        super(String.format("no mapping for the name %s", message));
    }
}
