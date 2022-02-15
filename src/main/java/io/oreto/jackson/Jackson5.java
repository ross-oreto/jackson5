package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Jackson5 {
    static final String DEFAULT_NAME = Util.Str.EMPTY;

    private static Supplier<ObjectMapper> defaultSupplier = Jackson5.newSupplier();

    /**
     * Supply named Jackson5 with an ObjectMapper.
     * @param name Name of the new Jackson5
     * @param supplier ObjectMapper supplier
     */
    public static void supply(String name, Supplier<ObjectMapper> supplier) {
        Jackson5 jackson5 = new Jackson5(name, supplier.get());
        jacksons.put(name, jackson5);
    }
    /**
     * Set the default ObjectMapper supplier
     * Should be called before the Jackson5.get method
     * @param supplier Use the supplied ObjectMapper instead of the default one
     */
    public static void supply(Supplier<ObjectMapper> supplier) {
        Jackson5.defaultSupplier = supplier;
        supply(DEFAULT_NAME, Jackson5.defaultSupplier);
    }

    /**
     * Supply named Jackson5 with an ObjectMapper.
     * @param name Name of the new Jackson5
     * @param mapperConfig ObjectMapper configuration
     */
    public static void supply(String name, MapperConfig mapperConfig) {
        supply(name, newSupplier(mapperConfig));
    }

    /**
     * Set the default ObjectMapper supplier
     * @param mapperConfig ObjectMapper configuration
     */
    public static void supply(MapperConfig mapperConfig) {
        supply(DEFAULT_NAME, newSupplier(mapperConfig));
    }

    /**
     * Get a Jackson5 Object
     * @param name The name of the Jackson5
     * @return Jackson5 object with specified name
     * @throws NoSuchJackson5 If name does not exist
     */
    public static Jackson5 get(String name) {
        if (jacksons.containsKey(name))
            return jacksons.get(name);
        throw new NoSuchJackson5(String.format("No Jackson5 named %s has been supplied", name));
    }

    /**
     * Get the Jackson5 object by name or the default if name doesn't exist.
     * @param name The name of the Jackson5
     * @return Jackson5 object with specified name
     */
    public static Jackson5 getOrDefault(String name) {
        if (jacksons.containsKey(name))
            return jacksons.get(name);
        return get();
    }

    /**
     * Get a Jackson5 Object
     * @return new or existing Jackson5 object with specified name
     */
    public static Jackson5 get() {
        if (!jacksons.containsKey(DEFAULT_NAME))
            supply(defaultSupplier);
        return jacksons.get(DEFAULT_NAME);
    }

    /**
     * Create a new ObjectMapper supplier using the mapperConfig
     * @param mapperConfig Details how to configure the new ObjectMapper.
     * @return The new ObjectMapper supplier
     */
    public static Supplier<ObjectMapper> newSupplier(MapperConfig mapperConfig) {
        ObjectMapper objectMapper = new ObjectMapper();
        mapperConfig.modules().forEach(objectMapper::registerModule);
        objectMapper.registerModule(mapperConfig.timeModule());
        mapperConfig.features().forEach((objectMapper::configure));
        mapperConfig.serializationFeatures().forEach((objectMapper::configure));
        mapperConfig.deserializationFeatures().forEach((objectMapper::configure));
        mapperConfig.visibility().forEach((objectMapper::setVisibility));

        return () -> objectMapper;
    }

    /**
     * Create a new ObjectMapper supplier using the mapperConfig
     * @return The new ObjectMapper supplier
     */
    public static Supplier<ObjectMapper> newSupplier() {
        return newSupplier(MapperConfig.defaultConfig());
    }

    private static final Map<String, Jackson5> jacksons = new HashMap<>();

    // *****************************************************************************************************

    private final String name;
    private final ObjectMapper objectMapper;
    private boolean pretty;

    protected Jackson5(String name, ObjectMapper objectMapper) {
        this.name = name;
        this.objectMapper = objectMapper;
        this.pretty = false;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName();
    }

    private JsonRenderer renderer() {
        return new JsonRenderer(objectMapper, pretty);
    }

    /**
     * pretty print JSON string according to boolean parameter
     * @param pretty If true pretty print JSON
     * @return This Jackson5 object
     */
    public Jackson5 pretty(boolean pretty) {
        this.pretty = pretty;
        return this;
    }
    /**
     * pretty print JSON string
     * @return This Jackson5 object
     */
    public Jackson5 pretty() {
        return pretty(true);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @return JsonNode
     */
    public JsonNode json(Object o) throws JsonProcessingException {
        return renderer().json(o);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields Fields which are included/excluded in the JSON object
     * @return JsonNode
     */
    public JsonNode json(Object o, IFields fields) throws JsonProcessingException {
        return renderer().json(o, fields);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields fields which are included in the JsonNode object
     * @return JsonNode
     */
    public JsonNode json(Object o, String fields) throws JsonProcessingException {
        return renderer().json(o, Fields.Include(fields));
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String string(Object o) throws JsonProcessingException {
        return renderer().render(o);
    }

    /**
     *
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields Fields which are included/excluded in the JSON string
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String string(Object o, IFields fields) throws JsonProcessingException {
        return renderer().render(o, fields);
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields fields which are included in the JSON string
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String string(Object o, String fields) throws JsonProcessingException {
        return renderer().render(o, Fields.Include(fields));
    }

    /**
     * Convert Object to a Map
     * @param o Object to convert
     * @param fields fields which are included/excluded in the Map
     * @return Map representation of the Object o
     */
    public Map<String, Object> map(Object o, IFields fields) throws JsonProcessingException {
        return map(json(o, fields));
    }

    /**
     * Convert Object to a Map
     * @param o Object to convert
     * @return Map representation of object o
     */
    public Map<String, Object> map(Object o) {
        return objectMapper.convertValue(o, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Convert JSON String to a Map
     * @param s JSON String to convert
     * @param fields fields which are included/excluded in the Map
     * @return Map representation of the JSON string
     * @throws JsonProcessingException If errors occur during serialization
     */
    public Map<String, Object> map(String s, IFields fields) throws IOException {
        return map(json(s, fields));
    }

    /**
     * Convert JSON String to a Map
     * @param s JSON String to convert
     * @return Map representation of the JSON string
     * @throws JsonProcessingException If errors occur during serialization
     */
    public Map<String, Object> map(String s) throws IOException {
        return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Create new object type from value map using specified class type
     * @param o Value map to populate new object
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied map
     */
    public <T> T object(Object o, Class<T> tClass) {
        return objectMapper.convertValue(o, tClass);
    }

    /**
     * Create new object type from value map using specified class type
     * @param o Value map to populate new object
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @param fields fields which are included/excluded in the class T
     * @return New typed object representing the supplied map
     */
    public <T> T object(Object o, Class<T> tClass, IFields fields) throws IOException {
        return objectMapper.reader().readValue(renderer().json(o, fields), tClass);
    }

    /**
     * Create new object type from JSON string using specified class type
     * @param s JSON String to convert
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied JSON string
     */
    public <T> T object(CharSequence s, Class<T> tClass) throws IOException {
        return objectMapper.reader().readValue(s.toString(), tClass);
    }

    /**
     * Create new object type from JSON string using specified class type
     * @param s JSON String to convert
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @param fields fields which are included/excluded in the class T
     * @return New typed object representing the supplied JSON string
     */
    public <T> T object(CharSequence s, Class<T> tClass, IFields fields) throws JsonProcessingException {
        return object(json(s, fields), tClass);
    }
}
