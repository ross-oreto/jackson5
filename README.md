![](https://i.giphy.com/media/pUeXcg80cO8I8/giphy.webp)

### jackson5 
A fast json renderer to serialize json dynamically.

Why?
 - It often becomes tiresome and tedious to add custom serializers, jackson views, DTOs, etc.
 - JSON objects are already DTOs in their own right. 
 - It would be nice to just dynamically serialize json according to what the client wants. 
Jackson5 does just that by providing a DSL to specify the json structure.
  
### Requirements
 - jdk 8+
 - maven 3+

### Install
```
<dependency>
  <groupId>io.github.ross-oreto.jackson5</groupId>
  <artifactId>jackson5</artifactId>
  <version>1.0.5</version>
</dependency>
```

### The Jackson5 class
- Firstly this class is a static container for ObjectMappers. Map[String -> ObjectMapper]
- Jackson5 preserves thread safety by using the ObjectMapper reader()/writer() methods when possible and not calling any config methods post creation.
- It also provides a handy way to create new object mappers using a fluent MapperConfig object

Get a new Jackson5 using the get() method. Jackson5 will use its own ObjectMapper unless you supply it with one.
```
Jackson5 jackson5 = Jackson5.get();
```
If your application already has an ObjectMapper that you want to use.
```
ObjectMapper mapper;

Jackson5.supply(() -> mapper); 
Jackson5 jackson5 = Jackson5.get();
```
Or create new Jackson5 objects by specifying a name.
``` 
Jackson5.supply("foo", () -> mapper);
Jackson5 jackson5 = Jackson5.getOrDefault("foo");
```
Using MapperConfig to create a new ObjectMapper with different date time patterns
```
Jackson5.supply("j5", MapperConfig.defaultConfig()
        .dateFormat("MM/dd/yyyy")
        .timeFormat("HH:mm")
        .dateTimeFormat("MM/dd/yyyy HH:mm"));
Jackson5 jackson5 = Jackson5.getOrDefault("j5");
```

### Serialization Usage
 - get a new Jackson5 and serialize as normal json
```
Jackson5 jackson5 = Jackson5.get();
Map data = new HashMap<String, String>(){{ put("name", "Ryzen5"); put("description", "A CPU"); }};
String json = jackson5.serialize(data);
```
as JSON object
```
JsonNode json = jackson5.json(data);
```
as a Map<String, Object>
```
Pojo pojo = new Pojo();
Map<String, Object> json = jackson5.map(pojo); 
```

### Deserialization
``` 
Pojo pojo = jackson5.deserialize(data, Pojo.class);
```

### Fields DSL
- The Fields object DSL is designed almost identically to graphQL syntax. 
- include the name field.
```
String json = jackson5.serialize(data, "{ name }");
```
- exclude the name field.
```
jackson5.serialize(data, Fields.Exclude("name"));
```
- Only render first 10 elements
```
jackson5.serialize(data, "list[1:10]");
```
- Change the root of the tree. If say the data of interest is down in class hierarchy
```
jackson5.serialize(data, Fields.Root("content.people").include("name address"));
```
- Chaining together (This will look at only the first element in the collection)
```
jackson5.serialize(data, Fields.Root("[1]").include("{ name address { street state }}"));
```

For more advanced examples, look at src/test/io/oreto/jackson/Jackson5Test

Jackson 5 also supports CSV. src/test/io/oreto/jackson/CsvTest
``` 
List<Map<String, Object>> elements = new ArrayList<>();
elements.add(new LinkedHashMap<String, Object>(){{ put("name", "ross"); put("address", "Nashville, TN"); }});
String csv = Csv.asCsv(elements);
```

### Performance
- Jackson5 just uses a Jackson ObjectMapper behind the scenes.
- With no Fields DSL, it will be just as fast as any Jackson serialization/deserialization.
- When using the Fields DSL there is a little extra processing.
- Jackson5 first converts an object into a JsonNode tree, then uses clever algorithms to prune the tree according to the Fields DSL specification.
- The Jmh test cases which are included in the test package, demonstrate that Jackson5 is between .1 and .2 ms slower than straight Jackson.
- So that's 1/10 or 2/10 of a millisecond slower that is un-noticeable for a great deal of dynamic flexibility.

### Spring Integration
There are two options for integration Jackson5 into a Spring application 
- Direct Approach (Override the ObjectMapper and register a Jackson5 bean for injection)
```
@Bean
@Primary
public ObjectMapper objectMapper() {
    return MapperConfig.defaultConfig().build();
}

@Bean
Jackson5 jackson5() {
    Jackson5.supply(this::objectMapper);
    return Jackson5.get();
}
```
- Fully Integrated Approach (Uses a custom HttpMessageConverter)
Create a Response class to pass needed parameters such as Fields to Jackson5
```
public class Jackson5Response {
    public static Jackson5Response of(Object body) {
        return new Jackson5Response(body);
    }
    public static Jackson5Response empty() {
        return new Jackson5Response();
    }

    Object body;
    private Fields fields = Fields.Include("");
    private String name = "";
    private boolean pretty;

    public Jackson5Response() {
    }

    public Jackson5Response(Object body) {
        this.body = body;
    }

    public Jackson5Response fields(Fields fields) {
        this.fields = fields;
        return this;
    }
    public Jackson5Response name(String name) {
        this.name = name;
        return this;
    }
    public Jackson5Response name(Class<?> aClass) {
        return name(aClass.getName());
    }
    public Jackson5Response pretty(boolean pretty) {
        this.pretty = pretty;
        return this;
    }

    public Object getBody() {
        return body;
    }
    public Fields getFields() {
        return fields;
    }
    public String getName() {
        return name;
    }
    public boolean isPretty() {
        return pretty;
    }
}
```

The custom HttpMessageConverter
```
public class Jackson5HttpMessageConverter extends AbstractHttpMessageConverter<Object> {
    static List<MediaType> mediaTypes = new ArrayList<MediaType>() {{
        add(MediaType.APPLICATION_JSON);
    }};

    @Override
    protected boolean supports(Class<?> clazz) {
        return Jackson5Response.class.isAssignableFrom(clazz);
    }

    @Override
    protected Object readInternal(Class<?> clazz
            , HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        String text = new BufferedReader(
                new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        return Jackson5.getOrDefault(clazz).deserialize(text, clazz);
    }

    @Override
    protected void writeInternal(Object response
            , HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Jackson5Response j5Response = (Jackson5Response) response;
        outputMessage.getBody().write(
                Jackson5.getOrDefault(j5Response.getName())
                        .serialize(j5Response.getBody(), j5Response.getFields(), j5Response.isPretty())
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
        return mediaTypes;
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return mediaTypes.contains(mediaType);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return mediaType == null || mediaTypes.contains(mediaType);
    }
}
```

Register the converter with Spring
```
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    messageConverters.add(new Jackson5HttpMessageConverter());
}
```

Then in a controller return the jackson5 response
```
@GetMapping
public Jackson5Response example() {
    return Jackson5Response.of(data);
}
```