package io.oreto.jackson.pojos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pojo {
    private String name;
    private String description;
    private List<Pojo1> pojos;

    public Pojo() {}

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
    public List<Pojo1> getPojos() {
        return pojos;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPojos(List<Pojo1> pojos) {
        this.pojos = pojos;
    }
}
