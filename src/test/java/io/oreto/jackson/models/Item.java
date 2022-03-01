package io.oreto.jackson.models;

import java.math.BigDecimal;

public class Item {
    private Long id;
    private String name;
    private BigDecimal price;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Item withId(Long id) {
        this.id = id;
        return this;
    }

    public Item withName(String name) {
        this.name = name;
        return this;
    }

    public Item withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }
}
