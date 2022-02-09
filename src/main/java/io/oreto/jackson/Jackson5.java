package io.oreto.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Jackson5 {
    /**
     * Build a new Jackson5 Object
     * @return new Jackson5
     */
    public static Jackson5 build() {
       return new Jackson5();
    }

    /**
     * Build a new Jackson5 Object
     * @param objectMapper Use the supplied ObjectMapper instead of the default one
     * @param pretty Pretty print json
     * @return new Jackson5
     */
    public static Jackson5 build(ObjectMapper objectMapper, boolean pretty) {
        return new Jackson5(objectMapper, pretty);
    }

    /**
     * Build a new Jackson5 Object
     * @param objectMapper Use the supplied ObjectMapper instead of the default one
     * @return new Jackson5
     */
    public static Jackson5 build(ObjectMapper objectMapper) {
        return new Jackson5(objectMapper);
    }

    /**
     * Build a new Jackson5 Object
     * @param name The name of the supplied ObjectMapper
     * @param objectMapper The ObjectMapper
     * @param pretty Pretty print json
     * @return new Jackson5
     */
    public static Jackson5 build(String name, ObjectMapper objectMapper, boolean pretty) {
        return new Jackson5(name, objectMapper, pretty);
    }

    /**
     * Build a new Jackson5 Object
     * @param name The name of the supplied ObjectMapper
     * @param objectMapper The ObjectMapper
     * @return new Jackson5
     */
    public static Jackson5 build(String name, ObjectMapper objectMapper) {
        return new Jackson5(name, objectMapper);
    }

    /**
     * Build a new Jackson5 Object
     * @param name The name of the new ObjectMapper
     * @param pretty Pretty print json
     * @return new Jackson5
     */
    public static Jackson5 build(String name, boolean pretty) {
        return new Jackson5(name, pretty);
    }

    /**
     * Build a new Jackson5 Object
     * @param name The name of the new ObjectMapper
     * @return new Jackson5
     */
    public static Jackson5 build(String name) {
        return new Jackson5(name);
    }

    /**
     * Default configuration of the ObjectMapper
     * @param mapper ObjectMapper to configure
     * @return The configured ObjectMapper
     */
    public static ObjectMapper configure(ObjectMapper mapper) {
        return mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule()
                        .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerModule(new ParameterNamesModule());
    }

    /**
     * Get the default ObjectMapper
     * @return The default ObjectMapper
     */
    protected static ObjectMapper mapper() {
        mappers.putIfAbsent("", configure(new ObjectMapper()));
        return mappers.get("");
    }

    public static final Map<String, ObjectMapper> mappers = new HashMap<>();

    /**
     * Get the default ObjectReader
     * @return The default ObjectReader
     */
    public static ObjectReader reader() {
        return reader("");
    }

    /**
     * Get the default ObjectWriter
     * @return The default ObjectWriter
     */
    public static ObjectWriter writer() {
        return writer("");
    }

    /**
     * Get the ObjectReader with the specified name
     * @param name Name of the ObjectReader
     * @return The ObjectReader
     */
    public static ObjectReader reader(String name) {
        return mappers.get(name).reader();
    }

    /**
     * Get the ObjectWriter with the specified name
     * @param name Name of the ObjectWriter
     * @return The ObjectWriter
     */
    public static ObjectWriter writer(String name) {
        return mappers.get(name).writer();
    }

    /**
     * Serialize Object o to JSON
     * @param o The Object to serialize
     * @param pretty Pretty print the JSON if true
     * @return A JSON String
     * @throws JsonProcessingException If errors occur during serialization
     */
    public static String asString(Object o, boolean pretty) throws JsonProcessingException {
        return pretty
                ? mapper().writerWithDefaultPrettyPrinter().writeValueAsString(o)
                : mapper().writer().writeValueAsString(o);
    }

    /**
     * Serialize Object o to JSON
     * @param o The Object to serialize
     * @return A JSON String
     * @throws JsonProcessingException If errors occur during serialization
     */
    public static String asString(Object o) throws JsonProcessingException {
        return asString(o, false);
    }

    /**
     * Convert Object to JsonNode Object
     * @param o Object to convert
     * @return JsonNode
     */
    public static JsonNode asJson(Object o) { return mapper().valueToTree(o); }

    /**
     * Convert JSON string to a JsonNode Object
     * @param s JSON string
     * @return JsonNode
     * @throws JsonProcessingException If errors occur during serialization
     */
    public static JsonNode asJson(String s) throws JsonProcessingException {
        return mapper().reader().readTree(s);
    }

    /**
     * Convert JSON String to a Map
     * @param s JSON String to convert
     * @return Map representation of the JSON string
     * @throws JsonProcessingException If errors occur during serialization
     */
    public static Map<String, Object> asMap(String s) throws JsonProcessingException {
        return mapper().readValue(s, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Convert Object to a Map
     * @param o Object to convert
     * @return Map representation of the JSON string
     */
    public static Map<String, Object> asMap(Object o) {
        return mapper().convertValue(o, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Create new object type from value map using specified class type
     * @param o Value map to populate new object
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied map
     */
    public static <T> T from(Map<String, ?> o, Class<T> tClass) {
        return mapper().convertValue(o, tClass);
    }

    /**
     * Create new object type from JSON string using specified class type
     * @param s JSON String to convert
     * @param tClass Class type of the new object
     * @param <T> Type of the new object
     * @return New typed object representing the supplied JSON string
     */
    public static <T> T from(CharSequence s , Class<T> tClass) throws IOException {
        return mapper().reader().readValue(s.toString(), tClass);
    }

    private final JsonRenderer jsonRenderer;

    protected Jackson5(String name, ObjectMapper objectMapper, boolean pretty) {
        mappers.put(name, objectMapper);
        this.jsonRenderer = new JsonRenderer(objectMapper, pretty);
    }
    protected Jackson5(ObjectMapper objectMapper, boolean pretty) {
        this("", objectMapper, pretty);
    }
    protected Jackson5(ObjectMapper objectMapper) {
        this("", objectMapper);
    }
    protected Jackson5(String name, ObjectMapper objectMapper) {
        this(name, objectMapper, false);
    }
    protected Jackson5(String name, boolean pretty) {
        this(name, configure(new ObjectMapper()), pretty);
    }
    protected Jackson5(String name) {
        this(name, false);
    }
    /**
     * Default constructor
     */
    protected Jackson5() {
        this(configure(new ObjectMapper()), false);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @return JsonNode
     */
    public JsonNode json(Object o) {
        return jsonRenderer.json(o);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param struct Structure representing the object fields which are converted
     * @return JsonNode
     */
    public JsonNode json(Object o, Structurable struct) {
        return jsonRenderer.json(o, struct);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields fields which are included in the JsonNode object
     * @return JsonNode
     */
    public JsonNode json(Object o, String fields) {
        return jsonRenderer.json(o, Structure.of(fields));
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String render(Object o) throws JsonProcessingException {
        return jsonRenderer.render(o);
    }

    /**
     *
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param struct Structure representing the object fields which are serialized
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String render(Object o, Structurable struct) throws JsonProcessingException {
        return jsonRenderer.render(o, struct);
    }

    /**
     * Serialize Object as JSON string
     * @param o The object to serialize
     * @param fields fields which are included in the JSON
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String render(Object o, String fields) throws JsonProcessingException {
        return jsonRenderer.render(o, Structure.of(fields));
    }
}
