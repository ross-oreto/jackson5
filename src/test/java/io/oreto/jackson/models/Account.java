package io.oreto.jackson.models;

import java.util.List;

public class Account {
    private String username;
    private String email;
    private List<String> logins;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getLogins() {
        return logins;
    }

    public void setLogins(List<String> logins) {
        this.logins = logins;
    }

    public Account withUsername(String username) {
        this.username = username;
        return this;
    }

    public Account withEmail(String email) {
        this.email = email;
        return this;
    }

    public Account withLogins(List<String> logins) {
        this.logins = logins;
        return this;
    }
}
