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
import java.sql.Time;
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
    static final String DEFAULT_NAME = Util.Str.EMPTY;
    static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    static String DEFAULT_TIME_PATTERN = "HH:mm:ss";
    static String DEFAULT_DATE_TIME_PATTERN = DEFAULT_DATE_PATTERN + " " + DEFAULT_TIME_PATTERN;

    private static Supplier<ObjectMapper> defaultSupplier = Jackson5::newObjectMapper;


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
     * Set the default date pattern.
     * @param datePattern The default date pattern String
     */
    public static void setDefaultDatePattern(String datePattern) {
        Jackson5.DEFAULT_DATE_PATTERN = datePattern;
    }
    /**
     * Set the default time pattern.
     * @param timePattern The default time pattern String
     */
    public static void setDefaultTimePattern(String timePattern) {
        Jackson5.DEFAULT_TIME_PATTERN = timePattern;
    }
    /**
     * Set the default datetime pattern.
     * @param dateTimePattern The default datetime pattern String
     */
    public static void setDefaultDateTimePattern(String dateTimePattern) {
        Jackson5.DEFAULT_DATE_TIME_PATTERN = dateTimePattern;
    }

    /**
     * Get a Jackson5 Object
     * @param name The name of the Jackson5
     * @return Jackson5 object with specified name and ObjectMapper
     * @throws NoSuchJackson5 If name does not exist
     */
    public static Jackson5 get(String name) {
        if (jacksons.containsKey(name))
            return jacksons.get(name);
        throw new NoSuchJackson5(String.format("No Jackson5 named %s has been supplied", name));
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
     * Create a new ObjectMapper using the supplied pattern strings.
     * @param datePattern Pattern string representing the date.
     * @param timePattern Pattern string representing the time.
     * @param dateTimePattern Pattern string representing the date+time.
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper(String datePattern
            , String timePattern
            , String dateTimePattern) {
        return registerTimeModule(new ObjectMapper()
                , datePattern, timePattern, dateTimePattern)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule());
    }

    /**
     * Create a new ObjectMapper using the supplied pattern strings.
     * @param datePattern Pattern string representing the date.
     * @param timePattern Pattern string representing the time.
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper(String datePattern, String timePattern) {
        return newObjectMapper(datePattern, timePattern, String.format("%s %s", datePattern, timePattern));
    }

    /**
     * Create a new ObjectMapper using the supplied date pattern string.
     * @param datePattern Pattern string representing the date.
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper(String datePattern) {
        return newObjectMapper(datePattern, DEFAULT_TIME_PATTERN);
    }
    /**
     * Create a new ObjectMapper using default settings
     * @return The new ObjectMapper
     */
    public static ObjectMapper newObjectMapper() {
        return newObjectMapper(DEFAULT_DATE_PATTERN);
    }

    /**
     * Register date time patterns with the object mapper and setup serializers/deserializers for them.
     * @param objectMapper Object mapper to register time patterns on
     * @param datePattern The pattern of date formats. java.util.Date and LocalDate
     * @param timePattern The pattern of time formats. java.sql.Time
     * @param dateTimePattern The pattern of date time formats. LocalDateTime
     * @return The ObjectMapper after registration
     */
    public static ObjectMapper registerTimeModule(ObjectMapper objectMapper
            , String datePattern
            , String timePattern
            , String dateTimePattern) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        DateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
        DateFormat timeFormatter = new SimpleDateFormat(timePattern);

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
                // java.sql.Time
                .addSerializer(Time.class, new JsonSerializer<Time>() {
                    @Override
                    public void serialize(Time value, JsonGenerator gen, SerializerProvider serializers)
                            throws IOException {
                        gen.writeString(timeFormatter.format(value));
                    }
                })
                .addDeserializer(Time.class, new JsonDeserializer<Time>() {
                    @Override
                    public Time deserialize(JsonParser p, DeserializationContext ctxt)
                            throws IOException {
                        try {
                            return new Time(dateFormat.parse(p.getText()).getTime());
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                })
       );
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
