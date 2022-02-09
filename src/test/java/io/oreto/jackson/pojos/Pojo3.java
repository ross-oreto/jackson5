package io.oreto.jackson.pojos;

public class Pojo3 {
    private final String name;
    private final Pojo pojo;

    public Pojo3(String name, Pojo pojo) {
        this.name = name;
        this.pojo = pojo;
    }
    public String getName() {
        return name;
    }
    public Pojo getPojo() {
        return pojo;
    }
}
