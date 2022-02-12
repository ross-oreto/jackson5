package io.oreto.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Jackson5 {
    public static final String DEFAULT_NAME = Util.Str.EMPTY;
    public static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    /**
     * Set the default ObjectMapper
     * @param objectMapper Use the supplied ObjectMapper instead of the default one
     */
    public static void setDefaultMapper(ObjectMapper objectMapper) {
        if (jacksons.containsKey(DEFAULT_NAME)) {
            jacksons.get(DEFAULT_NAME).jsonRenderer.objectMapper = objectMapper;
        } else {
            get(DEFAULT_NAME, () -> objectMapper);
        }
    }

    /**
     * Set the default date pattern. Only affects the default ObjectMapper
     * @param datePattern The default date pattern String
     */
    public static void setDefaultDatePattern(String datePattern) {
        Jackson5.DEFAULT_DATE_PATTERN = datePattern;
    }
    /**
     * Set the default time pattern. Only affects the default ObjectMapper
     * @param timePattern The default time pattern String
     */
    public static void setDefaultTimePattern(String timePattern) {
        Jackson5.DEFAULT_TIME_PATTERN = timePattern;
    }

    /**
     * Get a Jackson5 Object
     * @param name The name of the Jackson5
     * @param objectMapperSupplier The ObjectMapper supplier
     * @return new or existing Jackson5 object with specified name and ObjectMapper
     */
    public static Jackson5 get(String name, Supplier<ObjectMapper> objectMapperSupplier) {
        if (jacksons.containsKey(name)) {
            return jacksons.get(name);
        } else {
            Jackson5 jackson5 = new Jackson5(objectMapperSupplier.get());
            jacksons.put(name, jackson5);
            return jackson5;
        }
    }

    /**
     * Get a Jackson5 Object
     * @return new or existing Jackson5 object with specified name
     */
    public static Jackson5 get() {
        return get(DEFAULT_NAME, Jackson5::newObjectMapper);
    }


    /**
     * Default configuration of the ObjectMapper
     * @return The new configured ObjectMapper
     */
    public static ObjectMapper newObjectMapper(String datePattern, String dateTimePattern) {
        return registerTimeModule(new ObjectMapper(), datePattern, dateTimePattern)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule());
    }

    /**
     * Register date time patterns with the object mapper and setup serializers/deserializers for them.
     * @param objectMapper Object mapper to register time patterns on
     * @param datePattern The pattern of date formats
     * @param dateTimePattern The pattern of date time formats
     * @return The ObjectMapper after registration
     */
    public static ObjectMapper registerTimeModule(ObjectMapper objectMapper
            , String datePattern
            , String dateTimePattern) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        DateFormat dateFormat = new SimpleDateFormat(dateTimePattern);

        return objectMapper.registerModule(new JavaTimeModule()
                .addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter))
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter))
                // java.util.Date
                .addSerializer(Date.class, new JsonSerializer<Date>() {
                    @Override
                    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers)
                            throws IOException {
                        gen.writeString(dateFormat.format(value));
                    }
                })
                .addDeserializer(Date.class, new JsonDeserializer<Date>() {
                    @Override
                    public Date deserialize(JsonParser p, DeserializationContext ctxt)
                            throws IOException {
                        try {
                            return dateFormat.parse(p.getText());
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                })
                // java.sql.Date
                .addSerializer(java.sql.Date.class, new JsonSerializer<java.sql.Date>() {
                    @Override
                    public void serialize(java.sql.Date value
                            , JsonGenerator gen
                            , SerializerProvider serializers) throws IOException {
                        gen.writeString(dateFormat.format(value));
                    }
                })
                .addDeserializer(java.sql.Date.class, new JsonDeserializer<java.sql.Date>() {
                    @Override
                    public java.sql.Date deserialize(JsonParser p, DeserializationContext ctxt)
                            throws IOException {
                        try {
                            return new java.sql.Date(dateFormat.parse(p.getText()).getTime());
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                })
       );
    }

    /**
     * Create a new ObjectMapper using the supplied date pattern string.
     * @param datePattern Pattern string representing the date.
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper(String datePattern) {
       return newObjectMapper(datePattern, String.format("%s %s", datePattern, DEFAULT_TIME_PATTERN));
    }
    /**
     * Create a new ObjectMapper using default settings
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper() {
        return newObjectMapper(DEFAULT_DATE_PATTERN);
    }

    /**
     * Get the specified ObjectMapper
     * @param name Name of the ObjectMapper
     * @return The ObjectMapper named 'name'
     */
    protected static ObjectMapper mapper(String name) {
        return get(name, Jackson5::newObjectMapper).getMapper();
    }
    /**
     * Get the default ObjectMapper
     * @return The default ObjectMapper
     */
    protected static ObjectMapper mapper() {
        return get().getMapper();
    }

    private static final Map<String, Jackson5> jacksons = new HashMap<>();

    /**
     * Get the ObjectReader with the specified name
     * @param name Name of the ObjectReader
     * @return The ObjectReader
     */
    public static ObjectReader reader(String name) {
        return mapper(name).reader();
    }

    /**
     * Get the ObjectWriter with the specified name
     * @param name Name of the ObjectWriter
     * @return The ObjectWriter
     */
    public static ObjectWriter writer(String name) {
        return mapper(name).writer();
    }

    /**
     * Get the default ObjectReader
     * @return The default ObjectReader
     */
    public static ObjectReader reader() {
        return mapper().reader();
    }

    /**
     * Get the default ObjectWriter
     * @return The default ObjectWriter
     */
    public static ObjectWriter writer() {
        return mapper().writer();
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

    // *****************************************************************************************************
    private final JsonRenderer jsonRenderer;

    protected Jackson5(ObjectMapper objectMapper) {
        this.jsonRenderer = new JsonRenderer(objectMapper, false);
    }

    private ObjectMapper getMapper() {
        return jsonRenderer.objectMapper;
    }

    /**
     * pretty print JSON string according to boolean parameter
     * @param pretty If true pretty print JSON
     * @return This Jackson5 object
     */
    public Jackson5 pretty(boolean pretty) {
        jsonRenderer.pretty(pretty);
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
    public JsonNode json(Object o) {
        return jsonRenderer.json(o);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param struct Fields representing the object fields which are converted
     * @return JsonNode
     */
    public JsonNode json(Object o, IFields struct) {
        return jsonRenderer.json(o, struct);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields fields which are included in the JsonNode object
     * @return JsonNode
     */
    public JsonNode json(Object o, String fields) {
        return jsonRenderer.json(o, Fields.Include(fields));
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
     * @param struct Fields representing the object fields which are serialized
     * @return JSON String representing the Object o
     * @throws JsonProcessingException If errors occur during serialization
     */
    public String render(Object o, IFields struct) throws JsonProcessingException {
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
        return jsonRenderer.render(o, Fields.Include(fields));
    }
}
