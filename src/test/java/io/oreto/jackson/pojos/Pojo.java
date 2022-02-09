package io.oreto.jackson.pojos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pojo {
    private final String name;
    private String description;
    private final List<Pojo1> pojos;

    public Pojo(String name) {
        this.name = name;
        pojos = new ArrayList<>();
    }

    public Pojo(String name, String description) {
        this(name);
        this.description = description;
    }

    public Pojo withPojos(String... names) {
        for (String name : names) {
            pojos.add(new Pojo1(name));
        }
        return this;
    }

    public Pojo withPojos(Pojo1... pojos) {
        Collections.addAll(this.pojos, pojos);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
