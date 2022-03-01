package io.oreto.jackson.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Purchase {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime purchasedOn;
    private List<Item> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return items.stream().map(Item::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPurchasedOn() {
        return purchasedOn;
    }

    public void setPurchasedOn(LocalDateTime purchasedOn) {
        this.purchasedOn = purchasedOn;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Purchase withId(Long id) {
        this.id = id;
        return this;
    }

    public Purchase withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public Purchase withPurchasedOn(LocalDateTime purchasedOn) {
        this.purchasedOn = purchasedOn;
        return this;
    }

    public Purchase withItems(List<Item> items) {
        this.items = items;
        return this;
    }
}
