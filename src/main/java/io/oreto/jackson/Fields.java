package io.oreto.jackson;

/**
 * Implementation of IFields
 */
public class Fields implements IFields {
    public static Fields Root(String root) {
        return new Fields().root(root);
    }
    public static Fields Include(String include) {
        return new Fields().include(include);
    }
    public static Fields Exclude(String exclude) {
        return new Fields().exclude(exclude);
    }

    /**
     * default constructor
     */
    public Fields(){}

    private String root;
    private String include;
    private String exclude;


    // fluent setters
    public Fields root(String root) {
        this.root = root;
        return this;
    }
    public Fields include(String include) {
        this.include = include;
        return this;
    }
    public Fields exclude(String exclude) {
        this.exclude = exclude;
        return this;
    }

    /**
     * @return The field to set as the tree root
     */
    @Override
    public String root() {
        return root;
    }

    /**
     * @return The fields to include in the serialization
     */
    @Override
    public String include() {
        return include;
    }

    /**
     * @return The fields to exclude in the serialization
     */
    @Override
    public String exclude() {
        return exclude;
    }
}
