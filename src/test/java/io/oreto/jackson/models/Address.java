package io.oreto.jackson.models;

public class Address {
    private Long id;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private Integer zip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getZip() {
        return zip;
    }

    public void setZip(Integer zip) {
        this.zip = zip;
    }

    public Address withId(Long id) {
        this.id = id;
        return this;
    }

    public Address withLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public Address withLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public Address withCity(String city) {
        this.city = city;
        return this;
    }

    public Address withState(String state) {
        this.state = state;
        return this;
    }

    public Address withZip(Integer zip) {
        this.zip = zip;
        return this;
    }
}
