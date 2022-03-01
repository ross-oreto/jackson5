package io.oreto.jackson.models;

import java.util.List;

public class Person {
    private Long id;
    private String firstName;
    private String lastName;

    private List<Address> addresses;
    private Address primaryAddress;
    private List<Vehicle> vehicles;
    private List<Purchase> purchases;
    private Account account;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Address getPrimaryAddress() {
        return primaryAddress;
    }

    public void setPrimaryAddress(Address primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<Purchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Person withId(Long id) {
        this.id = id;
        return this;
    }

    public Person withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Person withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Person withAddresses(List<Address> addresses) {
        this.addresses = addresses;
        return this;
    }

    public Person withPrimaryAddress(Address primaryAddress) {
        this.primaryAddress = primaryAddress;
        return this;
    }

    public Person withVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
        return this;
    }

    public Person withPurchases(List<Purchase> purchases) {
        this.purchases = purchases;
        return this;
    }

    public Person withAccount(Account account) {
        this.account = account;
        return this;
    }
}
