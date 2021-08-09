package io.oreto.jackson.test.pojos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pojo1 {
    private final String name;
    private String description;
    private final List<Pojo2> pojos;

    public Pojo1(String name) {
        this.name = name;
        pojos = new ArrayList<>();
    }

    public Pojo1(String name, String description) {
        this(name);
        this.description = description;
    }

    public Pojo1 withPojos(String... names) {
        for (String name : names) {
            pojos.add(new Pojo2(name));
        }
        return this;
    }

    public Pojo1 withPojos(Pojo2... pojos) {
        Collections.addAll(this.pojos, pojos);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Pojo2> getPojos() {
        return pojos;
    }
}
