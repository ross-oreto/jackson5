package io.oreto.jackson;


public class Structure implements Structurable {
    public static Structure of(String select) {
        return new Structure().select(select);
    }

    public static Structure of() {
        return new Structure();
    }

    private Structure(){}

    private String view;
    private String select;
    private String drop;

    public Structure view(String view) {
        this.view = view;
        return this;
    }
    public Structure select(String select) {
        this.select = select;
        return this;
    }
    public Structure drop(String drop) {
        this.drop = drop;
        return this;
    }

    @Override
    public String view() {
        return view;
    }

    @Override
    public String select() {
        return select;
    }

    @Override
    public String drop() {
        return drop;
    }
}
