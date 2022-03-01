package io.oreto.jackson.models;

public class Vehicle {
    private String vin;
    private String make;
    private String model;
    private Integer year;

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Vehicle withVin(String vin) {
        this.vin = vin;
        return this;
    }

    public Vehicle withMake(String make) {
        this.make = make;
        return this;
    }

    public Vehicle withModel(String model) {
        this.model = model;
        return this;
    }

    public Vehicle withYear(Integer year) {
        this.year = year;
        return this;
    }
}
