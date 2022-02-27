package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * Supply named Jackson5 with an ObjectMapper.
     * @param cls Class name is used to name the new Jackson5
     * @param supplier ObjectMapper supplier
     */
    public static void supply(Class<?> cls, Supplier<ObjectMapper> supplier) {
        supply(cls.getName(), supplier);
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
     * Supply named Jackson5 with an ObjectMapper.
     * @param cls Class name is used to name the new Jackson5
     * @param mapperConfig ObjectMapper configuration
     */
    public static void supply(Class<?> cls, MapperConfig mapperConfig) {
        supply(cls, newSupplier(mapperConfig));
    }

    /**
     * Set the default ObjectMapper supplier
     * @param mapperConfig ObjectMapper configuration
     */
    public static void supply(MapperConfig mapperConfig) {
        supply(DEFAULT_NAME, newSupplier(mapperConfig));
    }

    /**
     * Get a Jackson5 Optional
     * @param name The name of the Jackson5
     * @return Optional Jackson5 object with specified name if the name exists.
     * If a name has not been supplied, <tt>Optional.empty()</tt>
     */
    public static Optional<Jackson5> find(String name) {
        return jacksons.containsKey(name) ? Optional.of(jacksons.get(name)) : Optional.empty();
    }

    /**
     * Get the Jackson5 object by name or the default if name doesn't exist.
     * @param name The name of the Jackson5
     * @return Jackson5 object with specified name
     */
    public static Jackson5 getOrDefault(String name) {
        return find(name).orElse(get());
    }

    /**
     * Get the Jackson5 object by name or throw NoSuchJackson5 exception.
     * @param name The name of the Jackson5
     * @return Jackson5 object with specified name
     * @throws NoSuchJackson5 If no mapping for the name exists
     */
    public static Jackson5 getOrThrow(String name) throws NoSuchJackson5 {
        return find(name).orElseThrow(() -> new NoSuchJackson5(name));
    }

    /**
     * Get a Jackson5 Optional
     * @param cls Class name used to look up the Jackson5
     * @return Optional Jackson5 object with specified name if the name exists.
     * If a name has not been supplied, <tt>Optional.empty()</tt>
     */
    public static Optional<Jackson5> find(Class<?> cls) {
        return find(cls.getName());
    }

    /**
     * Get the Jackson5 object by name or the default if name doesn't exist.
     * @param cls Class name used to look up the Jackson5
     * @return Jackson5 object with specified name
     */
    public static Jackson5 getOrDefault(Class<?> cls) {
        return getOrDefault(cls.getName());
    }

    /**
     * Get the Jackson5 object by name or throw NoSuchJackson5 exception.
     * @param cls Class name used to look up the Jackson5
     * @return Jackson5 object with specified name
     * @throws NoSuchJackson5 If no mapping for the name exists
     */
    public static Jackson5 getOrThrow(Class<?> cls) throws NoSuchJackson5 {
        return getOrThrow(cls.getName());
    }

    /**
     * Get a Jackson5 Object
     * @return new or existing Jackson5 object
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
        return mapperConfig::build;
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
    private final ObjectMapper mapper;
    private final FieldsDSL fieldsDSL;

    protected Jackson5(String name, ObjectMapper mapper) {
        this.name = name;
        this.mapper = mapper;
        this.fieldsDSL = new FieldsDSL(mapper);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName();
    }

    private FieldsDSL renderer() {
        return fieldsDSL;
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @return JsonNode
     * @throws JsonProcessingException If errors occur during serialization
     */
    public JsonNode json(Object o) throws JsonProcessingException {
        return renderer().json(o);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields Fields which are included/excluded in the JSON object
     * @return JsonNode
     * @throws JsonProcessingException If errors occur during serialization
     */
    public JsonNode json(Object o, IFields fields) throws JsonProcessingException {
        return renderer().json(o, fields);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields fields which are included in the JsonNode object
     * @return JsonNode
     * @throws JsonProcessingException If errors occur during serialization
     */
    public JsonNode json(Object o, String fields) throws JsonProcessingException {
        return renderer().json(o, Fields.Include(fields));
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param pretty If true JSON string will be pretty printed, otherwise ugly
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String serialize(Object o, boolean pretty) throws JsonProcessingException {
        return pretty
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)
                : mapper.writer().writeValueAsString(o);
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields Fields which are included/excluded in the JSON string
     * @param pretty If true JSON string will be pretty printed, otherwise ugly
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String serialize(Object o, IFields fields, boolean pretty) throws JsonProcessingException {
        return pretty ? renderer().json(o, fields).toPrettyString() : renderer().json(o, fields).toString();
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields Fields which are included/excluded in the JSON string
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String serialize(Object o, IFields fields) throws JsonProcessingException {
        return renderer().json(o, fields).toString();
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields fields which are included in the JSON string
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String serialize(Object o, String fields) throws JsonProcessingException {
        return renderer().json(o, Fields.Include(fields)).toString();
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String serialize(Object o) throws JsonProcessingException {
        return mapper.writer().writeValueAsString(o);
    }

    /**
     * Convert Object to a Map
     * @param o Object to convert
     * @param fields fields which are included/excluded in the Map
     * @return Map representation of the Object o
     * @throws JsonProcessingException If errors occur during serialization
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
        return mapper.convertValue(o, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Convert JSON String to a Map
     * @param json JSON String to convert
     * @param fields fields which are included/excluded in the Map
     * @return Map representation of the JSON string
     * @throws IOException If errors occur during serialization
     */
    public Map<String, Object> map(CharSequence json, IFields fields) throws IOException {
        return map(json(json, fields));
    }

    /**
     * Convert JSON String to a Map
     * @param json JSON String to convert
     * @return Map representation of the JSON string
     * @throws IOException If errors occur during serialization
     */
    public Map<String, Object> map(CharSequence json) throws IOException {
        return mapper.readValue(json.toString(), new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Create new object type from value map using specified class type
     * @param o Value map to populate new object
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied map
     */
    public <T> T convert(Object o, Class<T> tClass) {
        return mapper.convertValue(o, tClass);
    }

    /**
     * Create new object type from value map using specified class type
     * @param o Value map to populate new object
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @param fields fields which are included/excluded in the class T
     * @return New typed object representing the supplied map
     * @throws IOException If errors occur during conversion
     */
    public <T> T convert(Object o, Class<T> tClass, IFields fields) throws IOException {
        return convert(renderer().json(o, fields), tClass);
    }

    /**
     * Create new object type from JSON string using specified class type
     * @param json JSON String to deserialize
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied JSON string
     * @throws IOException If errors occur during deserialization
     */
    public <T> T deserialize(CharSequence json, Class<T> tClass) throws IOException {
        return mapper.reader().readValue(json.toString(), tClass);
    }

    /**
     * Create new object type from JSON string using specified class type
     * @param json JSON String to deserialize
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @param fields fields which are included/excluded in the class T
     * @return New typed object representing the supplied JSON string
     * @throws JsonProcessingException If errors occur during serialization
     */
    public <T> T deserialize(CharSequence json, Class<T> tClass, IFields fields) throws IOException {
        return convert(json(json, fields), tClass);
    }

    /**
     * Convert an iterable collection into a new list of type T
     * @param iterable The collection being converted
     * @param tClass The type of list to return
     * @param <T> The type of the returned list
     * @return The resulting new typed list
     */
    public <T> List<T> convertCollection(Iterable<?> iterable, Class<?> tClass) {
        return mapper.convertValue(iterable, TypeFactory.defaultInstance().constructCollectionType(List.class, tClass));
    }

    /**
     * Convert an array into a new list of type T
     * @param array The array being converted
     * @param tClass The type of list to return
     * @param <T> The type of the returned list
     * @return The resulting new typed list
     */
    public <T> List<T> convertCollection(Object[] array, Class<?> tClass) {
        return mapper.convertValue(array, TypeFactory.defaultInstance().constructCollectionType(List.class, tClass));
    }

    /**
     * Convert an iterable collection into a new list of type T
     * @param iterable The collection being converted
     * @param tClass The type of list to return
     * @param <T> The type of the returned list
     * @param fields fields which are included/excluded in the class T
     * @return The resulting new typed list
     * @throws IOException If errors occur during conversion
     */
    public <T> List<T> convertCollection(Iterable<?> iterable, Class<T> tClass, IFields fields) throws IOException {
        return convertCollection(renderer().json(iterable, fields), tClass);
    }

    /**
     * Convert an array into a new list of type T
     * @param array The array being converted
     * @param tClass The type of list to return
     * @param <T> The type of the returned list
     * @param fields fields which are included/excluded in the class T
     * @return The resulting new typed list
     * @throws IOException If errors occur during conversion
     */
    public <T> List<T> convertCollection(Object[] array, Class<T> tClass, IFields fields) throws IOException {
        return convertCollection(renderer().json(array, fields), tClass);
    }

    /**
     * Create new List from the JSON string using the specified class type
     * @param json JSON String to deserialize
     * @param tClass Class type of the new list
     * @param <T> Type of the new list
     * @return New typed list representing the supplied JSON string
     * @throws IOException If errors occur during deserialization
     */
    public <T> List<T> deserializeCollection(CharSequence json, Class<T> tClass) throws IOException {
        return mapper.readValue(json.toString(), TypeFactory.defaultInstance().constructCollectionType(List.class, tClass));
    }

    /**
     * Create new List from the JSON string using the specified class type
     * @param json JSON String to deserialize
     * @param tClass Class type of the new list
     * @param <T> Type of the new list
     * @param fields fields which are included/excluded in the class T
     * @return New typed list representing the supplied JSON string
     * @throws IOException If errors occur during deserialization
     */
    public <T> List<T> deserializeCollection(CharSequence json, Class<T> tClass, IFields fields) throws IOException {
        return convertCollection(renderer().json(json, fields), tClass);
    }
}
