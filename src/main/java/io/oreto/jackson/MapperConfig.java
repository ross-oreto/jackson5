package io.oreto.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import java.util.*;

/**
 * Fluent config object for creating new ObjectMapper according to specifications of the MapperConfig
 */
public class MapperConfig {
    static String DEFAULT_DATE_PATTERN = "MM-dd-yyyy";
    static String DEFAULT_TIME_PATTERN = "HH:mm:ss";
    static String DEFAULT_DATE_TIME_PATTERN = DEFAULT_DATE_PATTERN + " " + DEFAULT_TIME_PATTERN;

    /**
     * Register date time patterns with the object mapper and setup serializers/deserializers for them.
     * @param datePattern The pattern of date formats. java.util.Date and LocalDate
     * @param timePattern The pattern of time formats. java.sql.Time
     * @param dateTimePattern The pattern of date time formats. LocalDateTime
     * @return The ObjectMapper after registration
     */
    static Module newTimeModule(String datePattern
            , String timePattern
            , String dateTimePattern) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        DateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
        DateFormat timeFormatter = new SimpleDateFormat(timePattern);

        return new JavaTimeModule()
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
                });
    }

    /**
     * The default configuration for a new ObjectMapper
     * @return MapperConfig
     */
    public static MapperConfig defaultConfig() {
        return new MapperConfig()
                .dateFormat(DEFAULT_DATE_PATTERN)
                .timeFormat(DEFAULT_TIME_PATTERN)
                .dateTimeFormat(DEFAULT_DATE_TIME_PATTERN)
                .module(new Jdk8Module())
                .module(new ParameterNamesModule())
                .feature(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .feature(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .feature(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
    /**
     * Empty configuration for a new ObjectMapper
     * @return MapperConfig
     */
    public static MapperConfig config() {
        return new MapperConfig();
    }

    private String dateFormat = DEFAULT_DATE_PATTERN;
    private String timeFormat = DEFAULT_TIME_PATTERN;
    private String dateTimeFormat = DEFAULT_DATE_TIME_PATTERN;
    private final SimpleModule module = new SimpleModule();
    private final List<Module> modules = new ArrayList<Module>() {{ add(module); }};
    private final Map<JsonParser.Feature, Boolean> features = new HashMap<>();
    private final Map<DeserializationFeature, Boolean> deserializationFeatures = new HashMap<>();
    private final Map<SerializationFeature, Boolean> serializationFeatures = new HashMap<>();
    private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new HashMap<>();
    private final Map<Class<?>, Class<?>> mixins = new HashMap<>();

    /**
     * Builds the new ObjectMapper according this configuration object.
     * @return A new ObjectMapper
     */
    public ObjectMapper build() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setMixIns(mixins());
        modules().forEach(objectMapper::registerModule);
        objectMapper.registerModule(timeModule());
        features().forEach((objectMapper::configure));
        serializationFeatures().forEach((objectMapper::configure));
        deserializationFeatures().forEach((objectMapper::configure));
        visibility().forEach((objectMapper::setVisibility));
        return objectMapper;
    }

    /**
     * @param dateFormat Pattern string representing the date
     * @return The MapperConfig
     */
    public MapperConfig dateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    /**
     * @param timeFormat Pattern string representing the time
     * @return The MapperConfig
     */
    public MapperConfig timeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
        return this;
    }

    /**
     * @param dateTimeFormat Pattern string representing the date+time
     * @return The MapperConfig
     */
    public MapperConfig dateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        return this;
    }

    /**
     * Add a custom serializer to the
     * @param tClass Class to provide serializer for
     * @param ser The serializer
     * @param <T> The type of class to serialize
     * @return The MapperConfig
     */
    public <T> MapperConfig serializer(Class<? extends T> tClass, JsonSerializer<T> ser) {
        module.addSerializer(tClass, ser);
        return this;
    }

    /**
     * Method to use for adding mix-in annotations to use for augmenting
     * specified class or interface. All annotations from
     * <code>mixinSource</code> are taken to override annotations
     * that <code>target</code> (or its supertypes) has.
     *
     * @param target Class (or interface) whose annotations to effectively override
     * @param mixinSource Class (or interface) whose annotations are to
     *   be "added" to target's annotations, overriding as necessary
     * @return The MapperConfig
     */
    public MapperConfig mixin(Class<?> target, Class<?> mixinSource) {
       this.mixins.put(target, mixinSource);
       return this;
    }

    /**
     * Method for registering a module that can extend functionality
     * provided by this mapper; for example, by adding providers for
     * custom serializers and deserializers.
     * @param module Module to register
     * @return The MapperConfig
     */
    public MapperConfig module(Module module) {
        this.modules.add(module);
        return this;
    }

    /**
     * Add feature to the configuration
     * @param feature JsonParser.Feature to add
     * @param state If true the feature is enabled, otherwise disabled
     * @return The MapperConfig
     */
    public MapperConfig feature(JsonParser.Feature feature, boolean state) {
        features.put(feature, state);
        return this;
    }

    /**
     * Add feature to the configuration
     * @param feature DeserializationFeature to add
     * @param state If true the feature is enabled, otherwise disabled
     * @return The MapperConfig
     */
    public MapperConfig feature(DeserializationFeature feature, boolean state) {
        deserializationFeatures.put(feature, state);
        return this;
    }

    /**
     * Add feature to the configuration
     * @param feature SerializationFeature to add
     * @param state If true the feature is enabled, otherwise disabled
     * @return The MapperConfig
     */
    public MapperConfig feature(SerializationFeature feature, boolean state) {
        serializationFeatures.put(feature, state);
        return this;
    }

    /**
     * Configure which properties are auto-detected
     * Such as <code>visibility(JsonMethod.FIELD, Visibility.ANY);</code>
     * @param propertyAccessor Type of property descriptor affected (field, getter/isGetter,
     *     setter, creator)
     * @param visibility Minimum visibility to require for the property descriptors of type
     * @return The MapperConfig
     */
    public MapperConfig visibility(PropertyAccessor propertyAccessor, JsonAutoDetect.Visibility visibility) {
       this.visibility.put(propertyAccessor, visibility);
       return this;
    }

    // package protected getters
    String dateFormat() {
       return dateFormat;
    }
    String timeFormat() {
        return timeFormat;
    }
    String dateTimeFormat() {
        return dateTimeFormat;
    }
    List<Module> modules() {
        return modules;
    }
    Map<Class<?>, Class<?>> mixins() {
        return mixins;
    }
    Module timeModule() {
        return newTimeModule(dateFormat(), timeFormat(), dateTimeFormat());
    }
    Map<JsonParser.Feature, Boolean> features() {
        return features;
    }
    Map<DeserializationFeature, Boolean> deserializationFeatures() {
        return deserializationFeatures;
    }
    Map<SerializationFeature, Boolean> serializationFeatures() {
        return serializationFeatures;
    }
    Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility() {
        return this.visibility;
    }
}
